package com.pwitko.ble.ops

import android.Manifest.permission.BLUETOOTH_CONNECT
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import com.pwitko.ble.exceptions.BleConnectionPermissionsNotGranted
import com.pwitko.ble.exceptions.BleGattOperationFailed
import com.pwitko.ble.gatt.BleGattManager
import com.pwitko.ble.gatt.CharacteristicNotification
import com.pwitko.ble.utils.CompositeId
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.ObservableEmitter
import java.util.*

internal class SubscribeOperation(
    private val bleGattManager: BleGattManager,
    private val characteristic: BluetoothGattCharacteristic
): BleOp<CharacteristicNotification>() {

    companion object {
        val CLIENT_CHARACTERISTIC_CONFIG_UUID: UUID =
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

        private val ENABLE_CONFIG = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        private val DISABLE_CONFIG = BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
    }

    override fun executeInternal(emitter: ObservableEmitter<CharacteristicNotification>) {
        val gatt = bleGattManager.bluetoothGatt
        val characteristicId = CompositeId(characteristic.uuid, characteristic.instanceId)
        val onCharacteristicChangedObservable = bleGattManager
            .onCharacteristicChangedObservable
            .filter { idDataPair -> idDataPair.first == characteristicId }
            .map { idDataPair -> idDataPair.second }

        val descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID)

        val notificationObservable = Observable.just(
            setNotificationCompletable(gatt,true)
                .andThen(writeCharacteristicDescriptorConfig(gatt, descriptor, ENABLE_CONFIG))
                .andThen(onCharacteristicChangedObservable)
        )

        emitter.onNext(CharacteristicNotification(characteristicId, notificationObservable))
        emitter.onComplete()
    }

    private fun setNotificationCompletable(
        gatt: BluetoothGatt,
        isEnabled: Boolean
    ): Completable {
        return Completable.fromAction {
            try {
                if(!gatt.setCharacteristicNotification(characteristic, isEnabled)) {
                    throw BleGattOperationFailed(BleOpType.SUBSCRIBE_CHARACTERISTIC)
                }
            } catch (exc: SecurityException) {
                throw BleConnectionPermissionsNotGranted(listOf(BLUETOOTH_CONNECT))
            }
        }
    }

    private fun writeCharacteristicDescriptorConfig(
        gatt: BluetoothGatt,
        descriptor: BluetoothGattDescriptor,
        data: ByteArray
    ): Completable {
        return Completable.create { completableEmitter ->
            val disposable = bleGattManager.onDescriptorWriteObservable
                .firstOrError()
                .ignoreElement()
                .subscribe { completableEmitter.onComplete() }
            completableEmitter.setDisposable(disposable)

            try {
                if (android.os.Build.VERSION.SDK_INT < 33) {
                    descriptor.value = data
                    gatt.writeDescriptor(descriptor)
                } else {
                    gatt.writeDescriptor(descriptor, data)
                }
            } catch (exc: SecurityException) {
                throw BleConnectionPermissionsNotGranted(listOf(BLUETOOTH_CONNECT))
            }
        }
    }
}