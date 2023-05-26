package com.pwitko.feature.blecomm.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.schedulers.Schedulers
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
annotation class IoScheduler

@Qualifier
annotation class AndroidMainScheduler

@Module
@InstallIn(SingletonComponent::class)
class SchedulersModule {

    @Singleton
    @IoScheduler
    @Provides
    fun provideIoScheduler(): Scheduler = Schedulers.io()

    @Singleton
    @AndroidMainScheduler
    @Provides
    fun provideMainScheduler(): Scheduler = AndroidSchedulers.mainThread()
}