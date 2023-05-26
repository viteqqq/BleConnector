package com.pwitko.ble.connection

import com.pwitko.ble.device.BleDevice
import com.pwitko.ble.gatt.BleGattManager
import com.pwitko.ble.ops.BleOpsFactory
import com.pwitko.ble.ops.OpsQueueImpl
import io.reactivex.rxjava3.schedulers.Schedulers
import java.util.concurrent.Executors

internal class BleConnectionFactoryImpl(private val opsFactory: BleOpsFactory): BleConnectionFactory {

    override fun createBleConnection(
        device: BleDevice,
        gattManager: BleGattManager
    ): BleConnection {
        val connectionOpsScheduler = Schedulers.from(Executors.newSingleThreadExecutor())
        val queue = OpsQueueImpl(connectionOpsScheduler)
        return BleConnectionImpl(device, gattManager, queue, opsFactory)
    }
}