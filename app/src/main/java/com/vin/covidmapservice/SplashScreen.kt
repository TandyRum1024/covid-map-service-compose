package com.vin.covidmapservice

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vin.covidmapservice.ui.theme.CovidMapServiceTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(
    viewmodel: MainViewModel = hiltViewModel(),
    onNavigateToMapScreen: () -> Unit
) {
    // Start the API loading
    LaunchedEffect(Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            var shouldRefreshCache = false

            // Check for cache
            val count = viewmodel.getDBCount()
            Log.d("DB", "viewmodel.getDBCount(): ${count}")
            if (count < 100) { // either not loaded at all or DB caching got cancelled halfway through
                shouldRefreshCache = true
            }

            // Callback
            val onDone: () -> Unit = {
                // Signal done
                viewmodel.splashProgress = 1f
                // Debug dump
                viewmodel.dumpDB()
                // Begin updating the fetched DB cache
                viewmodel.beginCollectingCachedCenter()

                // Move to the next screen!
                onNavigateToMapScreen()
            }

            viewmodel.updateAPICache(
                onProgressUpdate = { progress, isDone ->
                    viewmodel.splashProgress = progress
                },
                skipAPICall = !shouldRefreshCache,
                onDone = onDone
            )
        }
    }

    Column (
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Title
        Text("Welcome!")
        Spacer(modifier = Modifier.height(2.dp))
        // Progress
        CircularProgressIndicator()
        LinearProgressIndicator(progress = viewmodel.splashProgress)
    }
}

@Preview(showBackground = true, widthDp = 320, heightDp = 720)
@Composable
fun SplashPreview() {
    // Make dummy viewmodel
    val viewmodel = MainViewModelDummy(CenterCacheDB(null))

    CovidMapServiceTheme {
        CompositionLocalProvider(isInPreview provides true) { // Notify the preview state
            SplashScreen(viewmodel = viewmodel, onNavigateToMapScreen = {})
        }
    }
}