package com.pwitko.ble.ops

import android.Manifest
import android.bluetooth.BluetoothGattCharacteristic
import com.pwitko.ble.exceptions.BleConnectionPermissionsNotGranted
import com.pwitko.ble.gatt.BleGattManager
import io.reactivex.rxjava3.core.ObservableEmitter
import java.util.*

internal class WriteOperation(
    private val bleGattManager: BleGattManager,
    private val characteristic: BluetoothGattCharacteristic,
    private val data: ByteArray
): BleOp<ByteArray>() {
    override fun executeInternal(emitter: ObservableEmitter<ByteArray>) {
        bleGattManager.onCharacteristicWriteObservable
            .filter { uuidToData -> uuidToData.first == characteristic.uuid }
            .map(Pair<UUID, ByteArray>::second)
            .firstOrError()
            .subscribeWith(createDisposableSingleObserver(emitter))
            .let { emitter.setDisposable(it) }

        try {
            val gatt = bleGattManager.bluetoothGatt
            if (android.os.Build.VERSION.SDK_INT < 33) {
                characteristic.value = data
                characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                gatt.writeCharacteristic(characteristic)
            } else {
                gatt.writeCharacteristic(characteristic, data,
                    BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                )
            }
        } catch (exc: SecurityException) {
            emitter.tryOnError(BleConnectionPermissionsNotGranted(listOf(Manifest.permission.BLUETOOTH_CONNECT)))
        }
    }
}