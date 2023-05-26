package com.pwitko.ble.gatt

import android.bluetooth.BluetoothGatt

interface BleGattProvider {
    val bluetoothGatt: BluetoothGatt
}