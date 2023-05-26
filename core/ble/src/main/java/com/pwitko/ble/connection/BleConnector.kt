package com.pwitko.ble.connection

import com.pwitko.ble.device.BleDevice
import com.pwitko.ble.gatt.BleGattManager
import com.pwitko.ble.ops.BleOpsFactory
import com.pwitko.ble.ops.OpsQueue
import io.reactivex.rxjava3.core.Observable

internal interface BleConnector {
    fun establishConnection(device: BleDevice, autoConnect: Boolean = false): Observable<BleConnection>
}

internal class BleConnectorImpl(
    private val gattManager: BleGattManager,
    private val clientQueue: OpsQueue,
    private val opsFactory: BleOpsFactory,
    private val bleConnectionFactory: BleConnectionFactory
): BleConnector {
    override fun establishConnection(device: BleDevice, autoConnect: Boolean): Observable<BleConnection> {
        return clientQueue.schedule(opsFactory.createConnectionOperation(gattManager, device, autoConnect))
            .switchMap { clientQueue.schedule(opsFactory.createDiscoverServicesOperation(gattManager)) }
            .map { bleConnectionFactory.createBleConnection(device, gattManager,) }
            .retry(2)
    }
}