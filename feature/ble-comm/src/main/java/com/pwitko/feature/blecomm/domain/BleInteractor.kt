package com.pwitko.feature.blecomm.domain

import com.pwitko.ble.BleClient
import com.pwitko.ble.connection.BleConnection
import com.pwitko.common.Failure
import com.pwitko.common.IResult
import com.pwitko.common.Success
import com.pwitko.feature.blecomm.device.ble.BleDeviceWrapper
import com.pwitko.feature.blecomm.di.IoScheduler
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Scheduler
import java.util.*
import javax.inject.Inject

internal class BleInteractor @Inject constructor(
    private val bleClient: BleClient,
    @IoScheduler private val scheduler: Scheduler
): BleUseCases {
    override val btState: BleClient.BtState
        get() = bleClient.btState

    override fun getDevicesWithService(uuid: UUID): Observable<IResult<List<Device>>> {
        val deviceSet = mutableSetOf<Device>()
        return bleClient.scanForBleDevicesWithServices(listOf(uuid))
            .map<IResult<List<Device>>> { device ->
                deviceSet.add(BleDeviceWrapper(device))
                Success(deviceSet.toList())
            }
            .onErrorReturn { Failure(it) }
            .subscribeOn(scheduler)
    }

    override fun connectToBleDevice(deviceAddress: String): Completable {
        return bleClient.getBleDevice(deviceAddress)
            .connect(false)
            .ignoreElement()
            .subscribeOn(scheduler)
    }

    override fun observeConnectionState(deviceAddress: String): Observable<BleConnection.State> {
        return bleClient.getBleDevice(deviceAddress).observeConnectionState()
    }

    override fun disconnect(deviceAddress: String): Completable {
        return bleClient.getBleDevice(deviceAddress).disconnect()
    }

    override fun observeBtState(): Observable<BleClient.BtState> {
        return bleClient.observeBluetoothState()
    }
}