package com.pwitko.feature.blecomm.domain

import com.pwitko.common.IResult
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single

internal interface HeartRateSensorUseCases {
    fun writeHeartRateControlPoint(deviceAddress: String, value: Int): Single<IResult<Int>>
    fun readBodySensorLocation(deviceAddress: String): Single<IResult<Int>>
    fun subscribeToHearRate(deviceAddress: String): Observable<Observable<IResult<Int>>>
}