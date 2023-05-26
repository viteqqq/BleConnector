package com.pwitko.ble

import android.Manifest.permission.BLUETOOTH_CONNECT
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build.VERSION.SDK_INT
import android.os.ParcelUuid
import android.provider.Settings
import androidx.annotation.VisibleForTesting
import androidx.core.app.ActivityCompat
import com.jakewharton.rxrelay3.BehaviorRelay
import com.pwitko.ble.connection.BleConnectionFactoryImpl
import com.pwitko.ble.connection.BleConnectorImpl
import com.pwitko.ble.device.BleDevice
import com.pwitko.ble.device.BleDeviceImpl
import com.pwitko.ble.exceptions.BleAdapterOff
import com.pwitko.ble.exceptions.BleConnectionPermissionsNotGranted
import com.pwitko.ble.exceptions.BleInvalidDeviceAddress
import com.pwitko.ble.gatt.BleGattManager
import com.pwitko.ble.ops.BleOpsFactory
import com.pwitko.ble.ops.OpsQueue
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import java.util.*
import java.util.concurrent.Executors

internal class BleClientImpl(
    private val manager: BluetoothManager,
    private val clientOpsQueue: OpsQueue,
    private val opsFactory: BleOpsFactory,
    private val appContext: Context
): BleClient() {
    private val bluetoothAdapter: BluetoothAdapter = manager.adapter
    private val deviceCache: MutableMap<BluetoothDevice, BleDevice> = mutableMapOf()

    override val btState: BtState
        get() = updateAndGetBtState()

    @VisibleForTesting
    private val btStateRelay = BehaviorRelay.create<BtState>()

    private val receiver = object: BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)
            if (state == BluetoothAdapter.STATE_ON || state == BluetoothAdapter.STATE_OFF) {
                updateAndGetBtState()
            }
        }
    }.also { appContext.registerReceiver(it, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)) }

    override fun scanForBleDevicesWithServices(
        serviceUuids: List<UUID>
    ): Observable<BleDevice> {
        val settings = ScanSettings.Builder().build()
        val filters = serviceUuids.map {
            ScanFilter.Builder().setServiceUuid(ParcelUuid(it)).build()
        }
        return Observable.defer {
            clientOpsQueue.schedule(opsFactory.createScanOperation(bluetoothAdapter, settings, filters))
                .map { scanResult -> getBleDevice(scanResult.device.address) }
                .mergeWith(btStateRelay
                    .filter { it == BtState.BLUETOOTH_ADAPTER_OFF }
                    .firstOrError()
                    .flatMap { Single.error(BleAdapterOff) }
                )
        }
    }

    override fun observeBluetoothState(): Observable<BtState> = btStateRelay
        .doOnSubscribe { updateAndGetBtState() }

    override fun getBleDevice(deviceAddress: String): BleDevice {
        return if (BluetoothAdapter.checkBluetoothAddress(deviceAddress)) {
            val device = bluetoothAdapter.getRemoteDevice(deviceAddress)
            deviceCache.getOrPut(device) {
                // Currently the deps are injected manually, ultimately that would be Hilt/Dagger
                val gattScheduler = Schedulers.from(Executors.newSingleThreadExecutor())
                val gattManager = BleGattManager(this, gattScheduler)
                val connectionFactory = BleConnectionFactoryImpl(opsFactory)
                val connector = BleConnectorImpl(gattManager, clientOpsQueue, opsFactory, connectionFactory)
                BleDeviceImpl(device, this, gattManager, opsFactory, connector)
            }
        } else {
            throw BleInvalidDeviceAddress(deviceAddress)
        }
    }

    override fun getConnectedDevices(): Set<BleDevice> = try {
        manager.getConnectedDevices(BluetoothProfile.GATT)
            .map { device -> getBleDevice(device.address) }
            .toSet()
    } catch (exc: SecurityException) {
        throw BleConnectionPermissionsNotGranted(listOf(BLUETOOTH_CONNECT))
    }

    override fun clientQueue(): OpsQueue = clientOpsQueue

    override fun removeCachedDevice(device: BleDevice) {
        deviceCache.remove(device.bluetoothDevice)
    }

    private fun isLocationServiceOn(): Boolean {
        return if (SDK_INT < 31) {
            try {
                Settings.Secure.getInt(
                    appContext.contentResolver,
                    Settings.Secure.LOCATION_MODE
                ) != Settings.Secure.LOCATION_MODE_OFF
            } catch (exc: Throwable) {
                false
            }
        } else {
            // Not needed on API 31+, so pass true
            true
        }
    }

    private fun isBluetoothAdapterEnabled(): Boolean {
        return bluetoothAdapter.isEnabled
    }

    private fun areBtPermissionsGranted(): Boolean {
        val permissions = if (SDK_INT < 31) {
            listOf(android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            )
        } else {
            listOf(android.Manifest.permission.BLUETOOTH_SCAN)
        }
        return permissions.all {
            ActivityCompat.checkSelfPermission(appContext, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun updateAndGetBtState(): BtState {
        val btState = when {
            !isBluetoothAdapterEnabled() -> BtState.BLUETOOTH_ADAPTER_OFF
            !isLocationServiceOn() -> BtState.LOCATION_SERVICES_OFF
            !areBtPermissionsGranted() -> BtState.BT_PERMISSIONS_NOT_GRANTED
            else -> BtState.READY
        }
        btStateRelay.accept(btState)
        return btState
    }
}