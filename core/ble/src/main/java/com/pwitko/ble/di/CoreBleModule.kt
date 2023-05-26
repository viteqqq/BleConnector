package com.pwitko.ble.di

import android.content.Context
import com.pwitko.ble.BleClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CoreBleModule {

    @Singleton
    @Provides
    fun provideBleClient(@ApplicationContext appContext: Context): BleClient {
        return BleClient.obtain(appContext)
    }
}