package com.pwitko.ble.ops

import android.util.Log
import androidx.annotation.VisibleForTesting
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.ObservableEmitter
import io.reactivex.rxjava3.observers.DisposableSingleObserver
import java.util.*

enum class BleOpType {
    SERVICE_DISCOVERY,
    READ_CHARACTERISTIC,
    WRITE_CHARACTERISTIC,
    SUBSCRIBE_CHARACTERISTIC,
    READ_DESCRIPTOR,
    WRITE_DESCRIPTOR,
    REQUEST_MTU
}

internal sealed class BleOp<T: Any> {
    private val uuid: UUID = UUID.randomUUID()

    fun execute(): Observable<T> {
        return Observable.create { emitter ->
            try {
                executeInternal(emitter)
            } catch (throwable: Throwable) {
                emitter.tryOnError(throwable)
            }
        }
    }

    protected abstract fun executeInternal(emitter: ObservableEmitter<T>)

    protected fun createDisposableSingleObserver(
        emitter: ObservableEmitter<T>
    ): DisposableSingleObserver<T> {
        return object: DisposableSingleObserver<T>() {
            override fun onSuccess(t: T) {
                if (!emitter.isDisposed) {
                    emitter.onNext(t)
                    emitter.onComplete()
                }
            }
            override fun onError(e: Throwable) {
                emitter.tryOnError(e)
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BleOp<*>) return false

        if (uuid != other.uuid) return false

        return true
    }

    override fun hashCode(): Int {
        return uuid.hashCode()
    }
}

@VisibleForTesting
internal object NoOp : BleOp<Unit>() {
    override fun executeInternal(emitter: ObservableEmitter<Unit>) {
        emitter.onNext(Unit)
        emitter.onComplete()
    }
}