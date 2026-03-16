package de.muenchen.appcenter.nimux

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

/**
 * Entry point for the app. This class must be referenced in the AndroidManifest.
 */
@HiltAndroidApp
class NimuxApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // for logging use timber, only in debug mode
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}