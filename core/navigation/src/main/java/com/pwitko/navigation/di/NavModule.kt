package com.pwitko.navigation.di

import com.pwitko.navigation.NavManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NavModule {

    @Singleton
    @Provides
    fun providesNavigationManager() = NavManager
}