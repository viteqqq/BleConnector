package com.pwitko.feature.blecomm.domain

import android.bluetooth.BluetoothDevice
import com.pwitko.ble.connection.BleConnection
import com.pwitko.ble.device.BleDevice
import com.pwitko.ble.utils.Timeout
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single

data class FakeDevice(override val name: String?, override val address: String): Device

data class FakeBleDevice(
    override val name: String?,
    override val address: String,
    override val bluetoothDevice: BluetoothDevice
): BleDevice {

    override val bleConnectionState: BleConnection.State = BleConnection.State.DISCONNECTED

    override fun connect(autoConnect: Boolean, timeout: Timeout): Single<BleConnection> {
        throw UnsupportedOperationException()
    }

    override fun observeConnectionState(): Observable<BleConnection.State> {
        throw UnsupportedOperationException()
    }

    override fun disconnect(): Completable {
        throw UnsupportedOperationException()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FakeBleDevice) return false

        if (name != other.name) return false
        if (address != other.address) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name?.hashCode() ?: 0
        result = 31 * result + address.hashCode()
        return result
    }
}