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

//    @Test
//    fun delaySubscription_observeSourceWhenItemArrives() {
//        val single = Single.create { emitter ->
//            val disposable = Single.fromCallable<Int> {
//                println("Callable called")
//                10
//            }
//                .delaySubscription { observer ->
//                    Observable.just(1, 2,3,4)
//                        .delay(5, TimeUnit.SECONDS)
//                        .filter { number -> number == 4 }
//                        .doOnNext { observer.onComplete() }
//                        .take(1)
//                        .subscribe()
//                }
//                .doOnSubscribe { println("OnSubscribe") }
//                .doOnSuccess { println("Received $it") }
//                .subscribe { gatt ->
//                    emitter.onSuccess(gatt)
//                }
//            emitter.setDisposable(disposable)
//        }
//        single.test().await().assertValue(10)
//    }
}