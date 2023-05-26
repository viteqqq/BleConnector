package com.pwitko.feature.blecomm.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.jakewharton.rxrelay3.BehaviorRelay
import com.pwitko.ble.BleClient
import com.pwitko.ble.connection.BleConnection
import com.pwitko.common.Failure
import com.pwitko.common.IResult
import com.pwitko.feature.blecomm.device.ble.BleConstant
import com.pwitko.feature.blecomm.domain.BleUseCases
import com.pwitko.feature.blecomm.domain.HeartRateSensorUseCases
import com.pwitko.feature.blecomm.navigation.NavDirections
import com.pwitko.navigation.NavManager
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.observers.DisposableObserver
import javax.inject.Inject

@HiltViewModel
internal class HeartRateSensorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val heartRateSensorUseCases: HeartRateSensorUseCases,
    private val bleUseCases: BleUseCases,
    private val navManager: NavManager
): ViewModel() {
    val deviceAddress: String = checkNotNull(savedStateHandle["deviceAddress"])

    private val compositeDisposable = CompositeDisposable()

    val btStateObservable = BehaviorRelay.create<BleClient.BtState>().also { stateRelay ->
        bleUseCases.observeBtState()
            .subscribe(stateRelay)
            .let { compositeDisposable.add(it) }
    }

    val connectionState = BehaviorRelay.create<BleConnection.State>().also {
        bleUseCases.observeConnectionState(deviceAddress)
            .subscribe(it).let { connStateSubscription ->
                compositeDisposable.add(connStateSubscription)
            }
    }

    val controlPoint = BehaviorRelay.create<IResult<Int>>()
    val bodyLocation = BehaviorRelay.create<IResult<Int>>()
    val heartRate = BehaviorRelay.create<IResult<Int>>()
    val heartRateNotificationsEnabled = BehaviorRelay.create<Boolean>().also {
        it.accept(false)
    }

    init {
        connectAndSubscribeToHeartRate()
    }

    fun refreshBtState() {
        btStateObservable.accept(bleUseCases.btState)
    }

    fun connectClicked() {
        // Reconnecting requires new scan as peer device very often uses randomized BT addresses
        // and the one we have been connected to might be unreachable. We have to remember we are not bonded.
        navManager.navigate(
            NavDirections.Scan.create(BleConstant.HeartRateService.HR_SERVICE_UUID.toString())
        )
    }

    fun disconnectClicked() {
        bleUseCases.disconnect(deviceAddress)
            .doOnComplete { navManager.navigate(NavDirections.Home) }
            .subscribe()
    }

    fun readBodyLocationClicked() {
        readBodyLocation()
    }

    fun writeClicked(data: Int) {
        heartRateSensorUseCases.writeHeartRateControlPoint(deviceAddress, data)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(controlPoint)
            .let { compositeDisposable.add(it) }
    }

    private fun readBodyLocation() {
        heartRateSensorUseCases.readBodySensorLocation(deviceAddress)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(bodyLocation)
            .let { disposable -> compositeDisposable.add(disposable) }
    }

    private fun connectAndSubscribeToHeartRate() {
        heartRateSensorUseCases.subscribeToHearRate(deviceAddress)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeWith(object: DisposableObserver<Observable<IResult<Int>>>() {
                override fun onNext(notificationStream: Observable<IResult<Int>>) {
                    heartRateNotificationsEnabled.accept(true)
                    notificationStream.doFinally { heartRateNotificationsEnabled.accept(false) }
                        .subscribe(heartRate)
                        .let { disposable -> compositeDisposable.add(disposable) }
                }
                override fun onError(e: Throwable) {
                    heartRate.accept(Failure(e))
                }
                override fun onComplete() { }
            }).let { compositeDisposable.add(it) }
    }

    override fun onCleared() {
        compositeDisposable.dispose()
        super.onCleared()
    }
}