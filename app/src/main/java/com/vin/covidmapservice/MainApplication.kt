package com.vin.covidmapservice

import android.app.Application
import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// For Hilt DI -- Provides Entry point & method do 'initialize' the required CenterCacheDB (which MainViewModel uses)
@HiltAndroidApp
class MainApplication: Application()

@Module
@InstallIn(SingletonComponent::class)
class MainAppModule {
    @Provides
    @Singleton
    fun provideCenterCacheDB(@ApplicationContext context: Context) = CenterCacheDB(context)
}

