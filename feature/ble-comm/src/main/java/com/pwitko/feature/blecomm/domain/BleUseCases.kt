package com.pwitko.feature.blecomm.domain

import com.pwitko.ble.BleClient
import com.pwitko.ble.connection.BleConnection
import com.pwitko.common.IResult
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import java.util.*

internal interface BleUseCases {
    val btState: BleClient.BtState

    fun getDevicesWithService(uuid: UUID): Observable<IResult<List<Device>>>
    fun connectToBleDevice(deviceAddress: String): Completable
    fun observeConnectionState(deviceAddress: String): Observable<BleConnection.State>
    fun observeBtState(): Observable<BleClient.BtState>
    fun disconnect(deviceAddress: String): Completable
}