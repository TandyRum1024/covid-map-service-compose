package com.vin.covidmapservice.hilt;

import android.content.Context;
import com.vin.covidmapservice.data.CenterCacheDB
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn(SingletonComponent::class)
class MainAppModule {
    @Provides
    fun provideCenterCacheDB(@ApplicationContext context:Context) = CenterCacheDB(context)
}