package com.pwitko.ble.ops

import android.Manifest
import android.bluetooth.BluetoothGattCharacteristic
import com.pwitko.ble.exceptions.BleConnectionPermissionsNotGranted
import com.pwitko.ble.gatt.BleGattManager
import io.reactivex.rxjava3.core.ObservableEmitter
import java.util.*

internal class ReadOperation(
    private val bleGattManager: BleGattManager,
    private val characteristic: BluetoothGattCharacteristic
): BleOp<ByteArray>() {
    override fun executeInternal(emitter: ObservableEmitter<ByteArray>) {
        bleGattManager.onCharacteristicReadObservable
            .filter { uuidToData -> uuidToData.first == characteristic.uuid }
            .map(Pair<UUID, ByteArray>::second)
            .firstOrError()
            .subscribeWith(createDisposableSingleObserver(emitter))
            .let { emitter.setDisposable(it) }

        try {
            val gatt = bleGattManager.bluetoothGatt
            gatt.readCharacteristic(characteristic)
        } catch (exc: SecurityException) {
            emitter.tryOnError(BleConnectionPermissionsNotGranted(listOf(Manifest.permission.BLUETOOTH_CONNECT)))
        }
    }
}