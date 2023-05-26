package com.pwitko.feature.blecomm.domain

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single

interface HeartRateService {
    fun writeHeartRateControlPoint(deviceAddress: String, value: Int): Single<Int>

    fun readBodySensorLocation(deviceAddress: String): Single<Int>

    fun subscribeToHeartRate(deviceAddress: String): Observable<Observable<Int>>
}