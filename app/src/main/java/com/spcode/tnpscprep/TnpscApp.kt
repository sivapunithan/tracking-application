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
        appScope.launch {
            runCatching { container.seedSyllabus() }
                .onSuccess { Log.i("TnpscApp", "Syllabus seed: $it") }
                .onFailure { Log.e("TnpscApp", "Syllabus seed failed", it) }
        }
    }
}
