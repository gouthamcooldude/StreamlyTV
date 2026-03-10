package com.streamlytv

import android.app.Application
import com.streamlytv.utils.PrefsManager
import com.streamlytv.worker.MetadataScanWorker

class StreamlyApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Schedule background scan if server is configured
        val prefs = PrefsManager(this)
        if (prefs.hasXtreamServer) {
            MetadataScanWorker.schedule(this)
        }
    }
}
