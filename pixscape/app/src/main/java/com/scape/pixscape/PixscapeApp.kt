package com.scape.pixscape

import android.app.Application
import android.util.Log
import com.scape.scapekit.LogLevel
import com.scape.scapekit.LogOutput
import com.scape.scapekit.Scape
import com.scape.scapekit.ScapeClient
import java.util.*

/**
 * Basic Application class that demonstrates the initialisation of the ScapeClient.
 *
 * ScapeClient entry point is acquired with ArSupport enabled and with our own Api Key.
 */
class PixscapeApp : Application() {

    companion object {
        private const val TAG = "PixscapeApp"

        private var mSharedInstance: PixscapeApp? = null
        var sharedInstance: PixscapeApp
            get() = mSharedInstance!!
            private set(value) {
                mSharedInstance = value
            }
    }

    lateinit var scapeClient: ScapeClient

    override fun onCreate() {
        super.onCreate()

        Log.i(TAG, "onCreate: Application created")

        sharedInstance = this

        scapeClient = Scape.scapeClientBuilder
                .withContext(applicationContext)
                .withDebugSupport(true)
                .withApiKey(BuildConfig.SCAPEKIT_API_KEY)
                .build()

        scapeClient.debugSession?.setLogConfig(LogLevel.LOG_DEBUG, EnumSet.of(LogOutput.CONSOLE))
        scapeClient.debugSession?.mockGPSCoordinates(51.553049, -0.124107)
    }
}