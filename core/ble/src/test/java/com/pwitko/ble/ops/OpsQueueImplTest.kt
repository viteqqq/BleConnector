package com.pwitko.ble.ops

import io.reactivex.rxjava3.schedulers.Schedulers
import org.junit.Test

class OpsQueueImplTest {

    @Test
    fun scheduleOps_testProcessedAndCompleted() {
        val queue = OpsQueueImpl(Schedulers.trampoline())
        queue.schedule(NoOp)
            .mergeWith(queue.schedule(NoOp))
            .mergeWith(queue.schedule(NoOp))
            .mergeWith(queue.schedule(NoOp))
            .test()
            .await()
            .assertValueCount(4)
            .assertComplete()
            .assertNoErrors()
    }
}