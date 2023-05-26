package com.pwitko.feature.blecomm.di

import com.pwitko.feature.blecomm.device.ble.HeartRateServiceImpl
import com.pwitko.feature.blecomm.domain.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

@Module
@InstallIn(ViewModelComponent::class)
abstract class BleCommModule {

    @Binds
    internal abstract fun bindHeartRateSensorService(impl: HeartRateServiceImpl): HeartRateService

    @Binds
    internal abstract fun bindBleUseCases(impl: BleInteractor): BleUseCases

    @Binds
    internal abstract fun bindHeartRateServiceUseCases(impl: HeartRateSensorInteractor): HeartRateSensorUseCases
}