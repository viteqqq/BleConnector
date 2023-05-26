package com.pwitko.ble.ops

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import android.content.Context
import androidx.core.content.getSystemService
import com.pwitko.ble.device.BleDevice
import com.pwitko.ble.gatt.BleGattManager

// Would be injected with component to handle operation provision. if time permits
internal class BleOpsFactoryImpl(
    private val context: Context,
    private val btManager: BluetoothManager
): BleOpsFactory {
    override fun createScanOperation(
        adapter: BluetoothAdapter,
        settings: ScanSettings,
        filters: List<ScanFilter>
    ): ScanOperation {
        return ScanOperation(adapter, settings, filters)
    }

    override fun createConnectionOperation(
        gattManager: BleGattManager,
        device: BleDevice,
        autoConnect: Boolean
    ): ConnectOperation {
        return ConnectOperation(device, gattManager, autoConnect, context)
    }

    override fun createWriteOperation(
        gattManager: BleGattManager,
        characteristic: BluetoothGattCharacteristic,
        data: ByteArray
    ): WriteOperation {
        return WriteOperation(gattManager, characteristic, data)
    }

    override fun createReadOperation(
        gattManager: BleGattManager,
        characteristic: BluetoothGattCharacteristic
    ): ReadOperation {
        return ReadOperation(gattManager, characteristic)
    }

    override fun createSubscribeOperation(
        gattManager: BleGattManager,
        characteristic: BluetoothGattCharacteristic
    ): SubscribeOperation {
        return SubscribeOperation(gattManager, characteristic)
    }

    override fun createDiscoverServicesOperation(gattManager: BleGattManager): DiscoverServicesOperation {
        return DiscoverServicesOperation(gattManager)
    }

    override fun createDescriptorWriteOperation(
        gattManager: BleGattManager,
        descriptor: BluetoothGattDescriptor,
        data: ByteArray
    ): WriteDescriptorOperation {
        return WriteDescriptorOperation(gattManager, descriptor, data)
    }

    override fun createDisconnectionOperation(gattManager: BleGattManager): DisconnectOperation {
        return DisconnectOperation(gattManager, btManager)
    }
}