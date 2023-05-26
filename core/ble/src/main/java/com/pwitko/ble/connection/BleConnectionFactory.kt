package com.pwitko.ble.connection

import com.pwitko.ble.device.BleDevice
import com.pwitko.ble.gatt.BleGattManager
import com.pwitko.ble.ops.OpsQueue

internal interface BleConnectionFactory {

    fun createBleConnection(
        device: BleDevice,
        gattManager: BleGattManager
    ): BleConnection
}