package com.pwitko.ble.connection

import android.bluetooth.BluetoothGattCharacteristic
import com.pwitko.ble.device.BleDevice
import com.pwitko.ble.gatt.BleServices
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single

interface BleConnection {
    val bleDevice: BleDevice
    val bleServices: BleServices

    enum class State {
        CONNECTING,
        CONNECTED,
        DISCONNECTING,
        DISCONNECTED
    }

    companion object {
        const val MIN_MTU: Int = 23
        const val MTU_ATT_OVERHEAD: Int = 3
        const val MIN_MTU_PAYLOAD_LENGTH = MIN_MTU - MTU_ATT_OVERHEAD
    }

    fun discoverServices(): Observable<BleServices>

    fun subscribe(characteristic: BluetoothGattCharacteristic): Observable<Observable<ByteArray>>

    fun read(characteristic: BluetoothGattCharacteristic): Single<ByteArray>

    fun write(characteristic: BluetoothGattCharacteristic, data: ByteArray): Single<ByteArray>
}