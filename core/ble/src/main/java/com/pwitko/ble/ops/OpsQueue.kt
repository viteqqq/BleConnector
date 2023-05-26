package com.pwitko.ble.ops

import io.reactivex.rxjava3.core.Observable

internal interface OpsQueue {
    fun <T: Any> schedule(op: BleOp<T>): Observable<T>
}