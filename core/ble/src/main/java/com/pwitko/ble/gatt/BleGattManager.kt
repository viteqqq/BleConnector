package com.pwitko.ble.gatt

import android.Manifest.permission.BLUETOOTH_CONNECT
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.util.Log
import com.jakewharton.rxrelay3.BehaviorRelay
import com.jakewharton.rxrelay3.PublishRelay
import com.pwitko.ble.BleClient
import com.pwitko.ble.connection.BleConnection
import com.pwitko.ble.exceptions.*
import com.pwitko.ble.ops.BleOpType
import com.pwitko.ble.utils.CompositeId
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Scheduler
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

internal class BleGattManager(
    private val bleClient: BleClient,
    private val gattScheduler: Scheduler): BleGattProvider {

    companion object {
        private fun mapConnectionState(state: Int) = when(state) {
            BluetoothGatt.STATE_CONNECTING -> BleConnection.State.CONNECTING
            BluetoothGatt.STATE_CONNECTED -> BleConnection.State.CONNECTED
            BluetoothGatt.STATE_DISCONNECTING -> BleConnection.State.DISCONNECTING
            else -> BleConnection.State.DISCONNECTED
        }
    }

    val onConnectionStateChangedObservable = BehaviorRelay.create<BleConnection.State>()

    private val servicesDiscoveredRelay = PublishRelay.create<BleServices>()
    private val characteristicReadRelay = PublishRelay.create<Pair<UUID, ByteArray>>()
    private val characteristicWriteRelay = PublishRelay.create<Pair<UUID, ByteArray>>()
    private val characteristicChangedRelay = PublishRelay.create<Pair<CompositeId, ByteArray>>().toSerialized()
    private val descriptorReadRelay = PublishRelay.create<Pair<UUID, ByteArray>>()
    private val descriptorWriteRelay = PublishRelay.create<Pair<UUID, ByteArray>>()
    private val connectionUpdatedRelay = PublishRelay.create<BleConnectionParameters>()
    private val mtuChangedRelay = PublishRelay.create<Int>()

    val disconnectionRelay = PublishRelay.create<BleException>()
    private val errorRelay = PublishRelay.create<BleException>()

    private val bluetoothGattRef = AtomicReference<BluetoothGatt>()

    init {
        disconnectionRelay.firstOrError().doFinally {
            val deviceAddress = bluetoothGatt.device.address
            try {
                bluetoothGatt.close()
            } catch (exc: SecurityException) {
                throw BleConnectionPermissionsNotGranted(listOf(BLUETOOTH_CONNECT))
            }
            bleClient.removeCachedDevice(bleClient.getBleDevice(deviceAddress))
        }.subscribe()
    }

    override val bluetoothGatt: BluetoothGatt
        get() = bluetoothGattRef.get()

//    val onConnectionStateChangedObservable: Observable<BleConnection.State>
//        get() = connectionStateChangedRelay.delay(0, TimeUnit.SECONDS, gattScheduler)

    val onServicesDiscoveredObservable: Observable<BleServices>
        get() = combineWithDisconnections(servicesDiscoveredRelay)
            .delay(0, TimeUnit.SECONDS, gattScheduler)

    val onCharacteristicReadObservable: Observable<Pair<UUID, ByteArray>>
        get() = combineWithDisconnections(characteristicReadRelay)
            .delay(0, TimeUnit.SECONDS, gattScheduler)

    val onCharacteristicWriteObservable: Observable<Pair<UUID, ByteArray>>
        get() = combineWithDisconnections(characteristicWriteRelay)
            .delay(0, TimeUnit.SECONDS, gattScheduler)

    val onDescriptorReadObservable: Observable<Pair<UUID, ByteArray>>
        get() = combineWithDisconnections(descriptorReadRelay)
            .delay(0, TimeUnit.SECONDS, gattScheduler)

    val onDescriptorWriteObservable: Observable<Pair<UUID, ByteArray>>
        get() = combineWithDisconnections(descriptorWriteRelay)
            .delay(0, TimeUnit.SECONDS, gattScheduler)

    @Suppress("unused")
    val onConnectionUpdatedObservable: Observable<BleConnectionParameters>
        get() = combineWithDisconnections(connectionUpdatedRelay)
            .delay(0, TimeUnit.SECONDS, gattScheduler)

    @Suppress("unused")
    val onMtuChangedObservable: Observable<Int>
        get() = combineWithDisconnections(mtuChangedRelay)
            .delay(0, TimeUnit.SECONDS, gattScheduler)

    val onCharacteristicChangedObservable: Observable<Pair<CompositeId, ByteArray>>
        get() = Observable.merge(
            characteristicChangedRelay,
            disconnectionRelay.flatMap { Observable.error(it) }
        ).delay(0, TimeUnit.SECONDS, gattScheduler)

    private fun <T: Any> combineWithDisconnections(observable: Observable<T>): Observable<T> {
        return Observable.merge(
            disconnectionRelay.flatMap { Observable.error(it) },
            observable,
            errorRelay.flatMap { Observable.error(it) }
        )
    }

    val gattCallback = object: BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            Log.d("BLEConnector", "onConnectionStateChanged: status=$status, state=${mapConnectionState(newState)}")
            updateBluetoothGatt(gatt)
            gattScheduler.createWorker().schedule {
                onConnectionStateChangedObservable.accept(mapConnectionState(newState))
            }

            if (isSuccess(status)) {
                if (isNotConnectedOrConnected(newState)) {
                    // Disconnecting gracefully
                    disconnectionRelay.accept(BleDisconnection(gatt.device.address, status))
                }
            } else {
                // GATT error
                disconnectionRelay.accept(BleGattDisconnection(gatt.device.address, status))
            }
        }

        private fun isSuccess(code: Int): Boolean = code == BluetoothGatt.GATT_SUCCESS

        private fun isNotConnectedOrConnected(newState: Int): Boolean {
            return newState == BluetoothGatt.STATE_DISCONNECTING ||
                    newState == BluetoothGatt.STATE_DISCONNECTED
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (isSuccess(status)) {
                servicesDiscoveredRelay.accept(BleServices(gatt.services.toSet()))
            } else {
                errorRelay.accept(BleGattOperationFailed(BleOpType.SERVICE_DISCOVERY))
            }
        }

        @Deprecated("Deprecated in API 33")
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) = onCharacteristicRead(gatt, characteristic, characteristic.value, status)

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int
        ) {
            if (isSuccess(status)) {
                characteristicReadRelay.accept(characteristic.uuid to value)
            } else {
                errorRelay.accept(BleGattOperationFailed(BleOpType.READ_CHARACTERISTIC))
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (isSuccess(status)) {
                characteristicWriteRelay.accept(characteristic.uuid to characteristic.value)
            } else {
                errorRelay.accept(BleGattOperationFailed(BleOpType.WRITE_CHARACTERISTIC))
            }
        }

        @Deprecated("Deprecated in API 33")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) = onCharacteristicChanged(gatt, characteristic, characteristic.value)

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            characteristicChangedRelay.accept(
                Pair(CompositeId(characteristic.uuid, characteristic.instanceId), value)
            )
        }

        @Deprecated("Deprecated in API 33")
        override fun onDescriptorRead(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) = onDescriptorRead(gatt, descriptor, status, descriptor.value)

        override fun onDescriptorRead(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int,
            value: ByteArray
        ) {
            if (isSuccess(status)) {
                descriptorReadRelay.accept(descriptor.uuid to value)
            } else {
                errorRelay.accept(BleGattOperationFailed(BleOpType.READ_DESCRIPTOR))
            }
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            if (isSuccess(status)) {
                descriptorWriteRelay.accept(descriptor.uuid to descriptor.value)
            } else {
                errorRelay.accept(BleGattOperationFailed(BleOpType.WRITE_DESCRIPTOR))
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            if (isSuccess(status)) {
                mtuChangedRelay.accept(mtu)
            } else {
                errorRelay.accept(BleGattOperationFailed(BleOpType.REQUEST_MTU))
            }
        }

        override fun onServiceChanged(gatt: BluetoothGatt) {
            servicesDiscoveredRelay.accept(BleServices(gatt.services.toSet()))
        }
    }

    fun updateBluetoothGatt(bluetoothGatt: BluetoothGatt) {
        bluetoothGattRef.set(bluetoothGatt)
    }
}