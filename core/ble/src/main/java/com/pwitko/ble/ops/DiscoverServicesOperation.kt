package com.pwitko.ble.ops

import com.pwitko.ble.exceptions.BleGattOperationFailed
import com.pwitko.ble.gatt.BleGattManager
import com.pwitko.ble.gatt.BleServices
import io.reactivex.rxjava3.core.ObservableEmitter

internal class DiscoverServicesOperation(
    private val bleGattManager: BleGattManager
    ): BleOp<BleServices>() {

    override fun executeInternal(emitter: ObservableEmitter<BleServices>) {
        val disposable = bleGattManager.onServicesDiscoveredObservable
            .firstOrError()
            .subscribe(
                { services ->
                    emitter.onNext(services)
                    emitter.onComplete()
                },
                { error -> emitter.tryOnError(error) }
            )
        emitter.setDisposable(disposable)

        try {
            if (!bleGattManager.bluetoothGatt.discoverServices()) {
                emitter.tryOnError(BleGattOperationFailed(BleOpType.SERVICE_DISCOVERY))
            }
        } catch (exc: SecurityException) {
            emitter.tryOnError(exc)
        }
    }
}