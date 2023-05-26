package com.pwitko.ble.device

import android.Manifest.permission.BLUETOOTH_CONNECT
import android.bluetooth.BluetoothDevice
import com.pwitko.ble.BleClient
import com.pwitko.ble.connection.BleConnection
import com.pwitko.ble.connection.BleConnector
import com.pwitko.ble.exceptions.BleConnectionPermissionsNotGranted
import com.pwitko.ble.gatt.BleGattManager
import com.pwitko.ble.ops.BleOpsFactory
import com.pwitko.ble.utils.Timeout
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import java.util.concurrent.atomic.AtomicReference

internal class BleDeviceImpl(
    override val bluetoothDevice: BluetoothDevice,
    private val client: BleClient,
    private val gattManager: BleGattManager,
    private val opsFactory: BleOpsFactory,
    private val connector: BleConnector
): BleDevice {

    override val address: String
        get() = bluetoothDevice.address
    override val name: String?
        get() = try {
            bluetoothDevice.name
        } catch (exc: SecurityException) {
            throw BleConnectionPermissionsNotGranted(listOf(BLUETOOTH_CONNECT))
        }

    private val connectionRef = AtomicReference<BleConnection>()

    override val bleConnectionState: BleConnection.State
        get() = gattManager.onConnectionStateChangedObservable.value ?:
            BleConnection.State.DISCONNECTED

    override fun connect(
        autoConnect: Boolean,
        timeout: Timeout
    ): Single<BleConnection> {
        val connectionSingle = connector.establishConnection(this)
            .firstOrError()
            .doOnSuccess { connection -> connectionRef.set(connection) }
        return getActiveConnection().switchIfEmpty(connectionSingle)
    }

    private fun getActiveConnection(): Maybe<BleConnection> {
        return Maybe.fromCallable {
            client.getConnectedDevices().firstOrNull { it.address == address }
        }.map { connectionRef.get() }
    }

    override fun observeConnectionState(): Observable<BleConnection.State> {
        return gattManager.onConnectionStateChangedObservable
    }

    override fun disconnect(): Completable {
        return client.clientQueue()
            .schedule(opsFactory.createDisconnectionOperation(gattManager))
//            .doOnTerminate { client.removeCachedDevice(this) }
            .ignoreElements()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BleDeviceImpl

        if (address != other.address) return false

        return true
    }

    override fun hashCode(): Int {
        return address.hashCode()
    }
}