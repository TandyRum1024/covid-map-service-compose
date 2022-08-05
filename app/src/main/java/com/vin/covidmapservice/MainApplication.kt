package com.vin.covidmapservice

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

// For Hilt DI -- Provides Entry point & method to 'initialize' the required CenterCacheDB (which MainViewModel uses)
@HiltAndroidApp
class MainApplication: Application()

