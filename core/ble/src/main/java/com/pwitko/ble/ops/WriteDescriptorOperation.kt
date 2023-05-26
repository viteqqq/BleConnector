package com.pwitko.ble.ops

import android.Manifest.permission.BLUETOOTH_CONNECT
import android.bluetooth.BluetoothGattDescriptor
import com.pwitko.ble.exceptions.BleConnectionPermissionsNotGranted
import com.pwitko.ble.gatt.BleGattManager
import io.reactivex.rxjava3.core.ObservableEmitter

internal class WriteDescriptorOperation(
    private val gattManager: BleGattManager,
    private val descriptor: BluetoothGattDescriptor,
    private val data: ByteArray
): BleOp<ByteArray>() {
    override fun executeInternal(emitter: ObservableEmitter<ByteArray>) {
        gattManager.onDescriptorWriteObservable
            .filter { pair -> pair.first == descriptor.uuid }
            .firstOrError().map { pair -> pair.second }
            .subscribeWith(createDisposableSingleObserver(emitter))
            .let { emitter.setDisposable(it) }

        try {
            if (android.os.Build.VERSION.SDK_INT < 33) {
                descriptor.value = data
                gattManager.bluetoothGatt.writeDescriptor(descriptor)
            } else {
                gattManager.bluetoothGatt.writeDescriptor(descriptor, data)
            }
        } catch (exc: SecurityException) {
            emitter.tryOnError(BleConnectionPermissionsNotGranted(listOf(BLUETOOTH_CONNECT)))
        }
    }
}