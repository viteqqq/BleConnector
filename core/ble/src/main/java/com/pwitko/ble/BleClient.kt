package com.pwitko.ble

import android.bluetooth.BluetoothManager
import android.content.Context
import com.pwitko.ble.device.BleDevice
import com.pwitko.ble.ops.BleOpsFactoryImpl
import com.pwitko.ble.ops.OpsQueue
import com.pwitko.ble.ops.OpsQueueImpl
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.util.*
import java.util.concurrent.Executors

abstract class BleClient {

    enum class BtState {
        BLUETOOTH_ADAPTER_OFF,
        BT_PERMISSIONS_NOT_GRANTED,
        LOCATION_SERVICES_OFF,
        READY
    }

    abstract val btState: BtState

    companion object {
        fun obtain(context: Context): BleClient {
            val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val opsScheduler = Schedulers.from(Executors.newSingleThreadExecutor())
            val clientOpsQueue = OpsQueueImpl(opsScheduler)
            val opsFactory = BleOpsFactoryImpl(context, manager)
            return BleClientImpl(manager, clientOpsQueue, opsFactory, context)
        }
    }

    abstract fun scanForBleDevicesWithServices(serviceUuids: List<UUID>): Observable<BleDevice>

    abstract fun observeBluetoothState(): Observable<BtState>

    abstract fun getBleDevice(deviceAddress: String): BleDevice

    abstract fun getConnectedDevices(): Set<BleDevice>

    internal abstract fun clientQueue(): OpsQueue

    internal abstract fun removeCachedDevice(device: BleDevice)
}