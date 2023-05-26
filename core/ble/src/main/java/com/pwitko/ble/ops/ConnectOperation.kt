package com.pwitko.ble.ops

import android.bluetooth.BluetoothDevice.PHY_LE_1M
import android.bluetooth.BluetoothDevice.TRANSPORT_LE
import android.bluetooth.BluetoothGatt
import android.content.Context
import android.os.Build.VERSION.SDK_INT
import android.os.Handler
import android.os.Looper
import com.pwitko.ble.connection.BleConnection
import com.pwitko.ble.device.BleDevice
import com.pwitko.ble.exceptions.BleConnectionError
import com.pwitko.ble.exceptions.BleConnectionPermissionsNotGranted
import com.pwitko.ble.exceptions.BleException
import com.pwitko.ble.gatt.BleGattManager
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.ObservableEmitter
import io.reactivex.rxjava3.core.Single

internal class ConnectOperation(
    private val device: BleDevice,
    private val bleGattManager: BleGattManager,
    private val autoConnect: Boolean,
    private val context: Context
): BleOp<BluetoothGatt>() {
    override fun executeInternal(emitter: ObservableEmitter<BluetoothGatt>) {
        val disconnectionObserver = bleGattManager.disconnectionRelay
            .flatMap { Observable.error<BleException>(it).cast(BluetoothGatt::class.java) }
            .firstOrError()
        bleGattManager.onConnectionStateChangedObservable
            .filter { state -> state == BleConnection.State.CONNECTED }
            .firstOrError()
//            .delay(500, TimeUnit.MILLISECONDS)
            .flatMap { Single.fromCallable { bleGattManager.bluetoothGatt } }
            .mergeWith(disconnectionObserver)
            .firstOrError()
            .subscribeWith(createDisposableSingleObserver(emitter))
            .let { emitter.setDisposable(it) }
        connectGatt()
    }

    private fun connectGatt(): BluetoothGatt = try {
        val gattCallback = bleGattManager.gattCallback
        val gatt = if (SDK_INT > 26) {
            device.bluetoothDevice
                .connectGatt(context, autoConnect, gattCallback, TRANSPORT_LE, PHY_LE_1M,
                    Handler(Looper.getMainLooper())) // callbacks only feed Relays so Main thread is ok
        } else {
            // don't pass Handler in API 26, there is a known bug in 8.0.0
            device.bluetoothDevice.connectGatt(context, autoConnect, gattCallback, TRANSPORT_LE)
        }

        if (gatt == null) {
            throw BleConnectionError("gatt is null")
        }

        bleGattManager.updateBluetoothGatt(gatt)
        gatt
    } catch (exc: SecurityException) {
        throw BleConnectionPermissionsNotGranted(listOf())
    }
}