package com.pwitko.ble.ops

import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.os.Build.VERSION.SDK_INT
import com.pwitko.ble.exceptions.BleAdapterOff
import com.pwitko.ble.exceptions.BleScanError
import com.pwitko.ble.exceptions.BleScanPermissionsNotGranted
import io.reactivex.rxjava3.core.ObservableEmitter

internal class ScanOperation(
    private val adapter: BluetoothAdapter,
    private val settings: ScanSettings,
    private val filters: List<ScanFilter>,
): BleOp<ScanResult>() {

    companion object {
        private val API26_SCAN_PERMISSIONS = listOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )

        private val API31_SCAN_PERMISSIONS = listOf(
            android.Manifest.permission.BLUETOOTH_SCAN
        )
    }

    override fun executeInternal(emitter: ObservableEmitter<ScanResult>) {
        val callback = createScanCallback(emitter)
        emitter.setCancellable {
            stopScan(emitter, callback)
        }
        startScan(emitter, callback)
    }

    private fun createScanCallback(emitter: ObservableEmitter<ScanResult>) =
        object: ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                result?.let { scanResult ->
                    emitter.onNext(scanResult)
                }
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>?) {
                results?.forEach { scanResult ->
                    emitter.onNext(scanResult)
                }
            }

            override fun onScanFailed(errorCode: Int) {
                emitter.tryOnError(BleScanError(errorCode))
            }
        }

    private fun startScan(emitter: ObservableEmitter<ScanResult>, scanCallback: ScanCallback) = try {
        adapter.bluetoothLeScanner?.startScan(filters, settings, scanCallback)
            ?: emitter.tryOnError(BleAdapterOff)
    } catch (exc: SecurityException) {
        val requiredPermissions = if (SDK_INT < 31) {
            API26_SCAN_PERMISSIONS
        } else {
            API31_SCAN_PERMISSIONS
        }
        emitter.tryOnError(BleScanPermissionsNotGranted(requiredPermissions))
    }

    private fun stopScan(emitter: ObservableEmitter<ScanResult>, scanCallback: ScanCallback) {
        try {
            adapter.bluetoothLeScanner?.stopScan(scanCallback)
        } catch (exc: SecurityException) {
            emitter.tryOnError(exc)
        } finally {
            if (!emitter.isDisposed) {
                emitter.onComplete()
            }
        }
    }
}