package com.pwitko.feature.blecomm.domain

import com.pwitko.common.Failure
import com.pwitko.common.IResult
import com.pwitko.common.Success
import com.pwitko.feature.blecomm.di.IoScheduler
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.core.Single
import javax.inject.Inject

internal class HeartRateSensorInteractor @Inject constructor(
    private val heartRateService: HeartRateService,
    @IoScheduler private val scheduler: Scheduler
): HeartRateSensorUseCases {
    override fun writeHeartRateControlPoint(deviceAddress: String, value: Int): Single<IResult<Int>> {
        return heartRateService.writeHeartRateControlPoint(deviceAddress, value)
            .subscribeOn(scheduler)
            .map<IResult<Int>> { Success(it) }
            .onErrorReturn { Failure(it) }
    }

    override fun readBodySensorLocation(deviceAddress: String): Single<IResult<Int>> {
        return heartRateService.readBodySensorLocation(deviceAddress)
            .subscribeOn(scheduler)
            .map<IResult<Int>> { Success(it) }
            .onErrorReturn { Failure(it) }
    }

    override fun subscribeToHearRate(deviceAddress: String): Observable<Observable<IResult<Int>>> {
        return heartRateService.subscribeToHeartRate(deviceAddress)
            .map { notificationsObservable -> notificationsObservable.map<IResult<Int>> { Success(it) }
                .onErrorReturn { Failure(it) } }
            .subscribeOn(scheduler)
    }
}