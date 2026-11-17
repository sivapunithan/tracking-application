package com.spcode.tnpscprep

import android.app.Application
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TnpscApp : Application() {
    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // Seeds on first launch; later launches (or an older asset version) are a cheap no-op.
        // Independent: a problem in one file must not stop the other from loading.
        appScope.launch {
            runCatching { container.seedSyllabus() }
                .onSuccess { Log.i("TnpscApp", "Syllabus seed: $it") }
                .onFailure { Log.e("TnpscApp", "Syllabus seed failed", it) }
            runCatching { container.seedAffairs() }
                .onSuccess { Log.i("TnpscApp", "Current affairs seed: $it") }
                .onFailure { Log.e("TnpscApp", "Current affairs seed failed", it) }
            // Optional headlines: does nothing when the user switched it off or fetched in the last 6 hours.
            runCatching { container.newsRepository.refresh(force = false) }
                .onSuccess { r -> Log.i("TnpscApp", "News refresh: " + (r?.joinToString { "${it.feedId}=${it.error ?: it.kept}" } ?: "skipped")) }
                .onFailure { Log.e("TnpscApp", "News refresh failed", it) }
        }
    }
}
