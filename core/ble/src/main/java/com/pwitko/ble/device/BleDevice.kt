package com.pwitko.ble.device

import android.bluetooth.BluetoothDevice
import com.pwitko.ble.connection.BleConnection
import com.pwitko.ble.utils.Timeout
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import java.util.concurrent.TimeUnit

interface BleDevice {
    val address: String
    val name: String?
    val bluetoothDevice: BluetoothDevice
    val bleConnectionState: BleConnection.State

    fun connect(
        autoConnect: Boolean = false,
        timeout: Timeout = Timeout(30, TimeUnit.SECONDS)
    ): Single<BleConnection>

    fun observeConnectionState(): Observable<BleConnection.State>

    fun disconnect(): Completable
}