package com.pwitko.ble.ops

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Scheduler
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.Semaphore

internal class OpsQueueImpl(private val workScheduler: Scheduler): OpsQueue {
    private val queue = LinkedBlockingQueue<WorkRunnable<*>>(100)
    private val semaphore = Semaphore(0)
    private val queueConsumerService = Executors.newSingleThreadExecutor()

    init {
        queueConsumerService.execute {
            while (true) {
                try {
                    val work = queue.take()
                    val worker = workScheduler.createWorker()
                    worker.schedule(work)
                    semaphore.acquire()
                    worker.dispose()
                } catch (exc: InterruptedException) {
                    exc.printStackTrace()
                }
            }
        }
    }

    override fun <T : Any> schedule(op: BleOp<T>): Observable<T> {
        return Observable.create { emitter ->
            val runnable = object: WorkRunnable<T>(op) {
                override fun run() {
                    if (emitter.isDisposed) {
                        semaphore.release()
                        return
                    }
                    val disposable = op.execute()
                        .unsubscribeOn(workScheduler)
                        .doFinally {
                            queue.remove(this)
                            semaphore.release()
                        }.subscribe(
                            { data -> emitter.onNext(data) },
                            { error -> emitter.tryOnError(error) },
                            { emitter.onComplete() }
                        )
                    emitter.setDisposable(disposable)
                }
            }
            queue.add(runnable)
        }
    }

    private abstract class WorkRunnable<T: Any>(
        val operation: BleOp<T>
    ): Runnable
}