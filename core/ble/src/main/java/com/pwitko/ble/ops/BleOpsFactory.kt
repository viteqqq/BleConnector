package com.pwitko.ble.ops

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import com.pwitko.ble.device.BleDevice
import com.pwitko.ble.gatt.BleGattManager

internal interface BleOpsFactory {

    fun createScanOperation(
        adapter: BluetoothAdapter,
        settings: ScanSettings,
        filters: List<ScanFilter>
    ): ScanOperation

    fun createConnectionOperation(
        gattManager: BleGattManager,
        device: BleDevice,
        autoConnect: Boolean
    ): ConnectOperation

    fun createWriteOperation(
        gattManager: BleGattManager,
        characteristic: BluetoothGattCharacteristic,
        data: ByteArray
    ): WriteOperation

    fun createReadOperation(
        gattManager: BleGattManager,
        characteristic: BluetoothGattCharacteristic
    ): ReadOperation

    fun createSubscribeOperation(
        gattManager: BleGattManager,
        characteristic: BluetoothGattCharacteristic
    ): SubscribeOperation

    fun createDescriptorWriteOperation(
        gattManager: BleGattManager,
        descriptor: BluetoothGattDescriptor,
        data: ByteArray
    ): WriteDescriptorOperation

    fun createDiscoverServicesOperation(gattManager: BleGattManager): DiscoverServicesOperation

    fun createDisconnectionOperation(gattManager: BleGattManager): DisconnectOperation
}