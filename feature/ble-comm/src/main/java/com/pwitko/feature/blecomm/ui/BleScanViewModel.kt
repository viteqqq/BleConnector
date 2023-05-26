package com.pwitko.feature.blecomm.ui

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.jakewharton.rxrelay3.BehaviorRelay
import com.pwitko.ble.BleClient
import com.pwitko.common.Failure
import com.pwitko.common.IResult
import com.pwitko.common.Success
import com.pwitko.feature.blecomm.domain.BleUseCases
import com.pwitko.feature.blecomm.domain.Device
import com.pwitko.feature.blecomm.navigation.NavDirections
import com.pwitko.navigation.NavManager
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.observers.DisposableObserver
import java.util.*
import javax.inject.Inject

@HiltViewModel
internal class BleScanViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val bleUseCases: BleUseCases,
    private val navManager: NavManager
) : ViewModel() {

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal var scanDisposable: Disposable? = null

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal val compositeDisposable = CompositeDisposable()

    private val serviceUuidString: String = checkNotNull(savedStateHandle["serviceUuid"])

    val btStateObservable = BehaviorRelay.create<BleClient.BtState>().also { stateRelay ->
        bleUseCases.observeBtState()
            .subscribe(stateRelay)
            .let { compositeDisposable.add(it) }
    }

    val foundDevices = BehaviorRelay.create<IResult<List<Device>>>()

    fun refreshBtState() {
        btStateObservable.accept(bleUseCases.btState)
    }

    fun discoverDevices() {
        bleUseCases.getDevicesWithService(UUID.fromString(serviceUuidString))
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeWith(object: DisposableObserver<IResult<List<Device>>>() {
                override fun onNext(t: IResult<List<Device>>) {
                    when(t) {
                        is Failure -> {
                            stopDiscovery()
                            foundDevices.accept(t)
                        }
                        is Success -> foundDevices.accept(t)
                        else -> Unit
                    }
                }
                override fun onError(e: Throwable) {
                    foundDevices.accept(Failure(e))
                    stopDiscovery()
                    refreshBtState()
                }
                override fun onComplete() { }
            }).let {
                scanDisposable = it
                compositeDisposable.add(it)
            }
    }

    fun stopDiscovery() {
        scanDisposable?.let { disposable ->
            disposable.dispose()
            compositeDisposable.remove(disposable)
            scanDisposable = null
        }
    }

    fun deviceSelected(device: Device) {
        stopDiscovery()
        navManager.navigate(NavDirections.HeartRate.create(device.address))
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    public override fun onCleared() {
        compositeDisposable.dispose()
        super.onCleared()
    }
}