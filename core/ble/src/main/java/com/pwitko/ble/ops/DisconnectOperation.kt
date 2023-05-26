package com.pwitko.ble.ops

import android.Manifest.permission.BLUETOOTH_CONNECT
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import com.pwitko.ble.exceptions.BleConnectionPermissionsNotGranted
import com.pwitko.ble.gatt.BleGattManager
import io.reactivex.rxjava3.core.CompletableObserver
import io.reactivex.rxjava3.core.ObservableEmitter
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable

internal class DisconnectOperation(
    private val bleGattManager: BleGattManager,
    private val btManager: BluetoothManager
): BleOp<Nothing>() {
    override fun executeInternal(emitter: ObservableEmitter<Nothing>) {
        val gatt = bleGattManager.bluetoothGatt
        disconnect(gatt).ignoreElement()
            .subscribe(object: CompletableObserver{
                override fun onSubscribe(d: Disposable) {
                    emitter.setDisposable(d)
                    disconnectGatt(gatt, emitter)
                }

                override fun onComplete() {
                    emitter.onComplete()
                }

                override fun onError(e: Throwable) {
                    emitter.tryOnError(e)
                }
            })
//            .let { emitter.setDisposable(it) }
//        disconnectGatt(gatt, emitter)
    }

    private fun isDisconnected(gatt: BluetoothGatt): Boolean = try {
        btManager.getConnectionState(gatt.device, BluetoothProfile.GATT) ==
                BluetoothProfile.STATE_DISCONNECTED
    } catch (exc: SecurityException) {
        throw BleConnectionPermissionsNotGranted(listOf(BLUETOOTH_CONNECT))
    }

    private fun disconnect(gatt: BluetoothGatt): Single<BluetoothGatt> {
        return if (isDisconnected(gatt)) {
            Single.just(gatt)
        } else {
            bleGattManager.disconnectionRelay
                .firstOrError()
                .map { bleGattManager.bluetoothGatt }
//                .doOnSuccess {
//                    try {
//                        gatt.close()
//                    } catch (exc: SecurityException) {
//                        throw BleConnectionPermissionsNotGranted(
//                            listOf(android.Manifest.permission.BLUETOOTH_CONNECT)
//                        )
//                    }
//                }
        }
    }

    private fun disconnectGatt(gatt: BluetoothGatt, emitter: ObservableEmitter<Nothing>) {
        try {
            gatt.disconnect()
        } catch (exc: SecurityException) {
            emitter.tryOnError(exc)
        }
    }
}