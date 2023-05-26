package com.pwitko.ble.connection

import android.bluetooth.BluetoothGattCharacteristic
import com.pwitko.ble.device.BleDevice
import com.pwitko.ble.exceptions.BleGattOperationFailed
import com.pwitko.ble.gatt.BleGattManager
import com.pwitko.ble.gatt.BleServices
import com.pwitko.ble.gatt.CharacteristicNotification
import com.pwitko.ble.ops.BleOpType
import com.pwitko.ble.ops.BleOpsFactory
import com.pwitko.ble.ops.OpsQueue
import com.pwitko.ble.utils.CompositeId
import com.pwitko.ble.utils.isNotifiable
import com.pwitko.ble.utils.isReadable
import com.pwitko.ble.utils.isWritable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import java.util.concurrent.ConcurrentHashMap

internal class BleConnectionImpl(
    override val bleDevice: BleDevice,
    private val gattManager: BleGattManager,
    private val opsQueue: OpsQueue,
    private val bleOpsFactory: BleOpsFactory
): BleConnection {

    private val activeNotificationsMap = ConcurrentHashMap<CompositeId, CharacteristicNotification>()

    override val bleServices: BleServices = BleServices(gattManager.bluetoothGatt.services.toSet())

    override fun discoverServices(): Observable<BleServices> {
        return opsQueue.schedule(bleOpsFactory.createDiscoverServicesOperation(gattManager))
    }

    override fun subscribe(characteristic: BluetoothGattCharacteristic): Observable<Observable<ByteArray>> {
        return if (characteristic.isNotifiable()) {
            val id = CompositeId(characteristic.uuid, characteristic.instanceId)
            if (activeNotificationsMap.contains(id)) {
                activeNotificationsMap[id]!!.observable
            } else {
                opsQueue.schedule(bleOpsFactory.createSubscribeOperation(gattManager, characteristic))
                    .flatMap { notification ->
                        activeNotificationsMap[notification.id] = notification
                        notification.observable
                    }
            }
        } else {
            Observable.error(BleGattOperationFailed(BleOpType.SUBSCRIBE_CHARACTERISTIC))
        }
    }

    override fun read(characteristic: BluetoothGattCharacteristic): Single<ByteArray> {
        return if (characteristic.isReadable()) {
            opsQueue
                .schedule(bleOpsFactory.createReadOperation(gattManager, characteristic))
                .firstOrError()
        } else {
            Single.error(BleGattOperationFailed(BleOpType.READ_CHARACTERISTIC))
        }
    }

    override fun write(
        characteristic: BluetoothGattCharacteristic,
        data: ByteArray
    ): Single<ByteArray> {
        return if (characteristic.isWritable()) {
            opsQueue
                .schedule(bleOpsFactory.createWriteOperation(gattManager, characteristic, data))
                .firstOrError()
        } else {
            Single.error(BleGattOperationFailed(BleOpType.WRITE_CHARACTERISTIC))
        }
    }
}