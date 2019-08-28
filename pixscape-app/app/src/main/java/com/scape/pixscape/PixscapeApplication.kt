package com.scape.pixscape

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.graphics.Color
import android.os.Build
import androidx.annotation.RequiresApi
import com.scape.scapekit.LogLevel
import com.scape.scapekit.LogOutput
import com.scape.scapekit.Scape
import com.scape.scapekit.ScapeClient
import java.util.*

class PixscapeApplication: Application() {

    private object PixscapeApplicationHolder {
        internal var INSTANCE: PixscapeApplication? = null
    }

    companion object {
        const val CHANNEL_ID = "timerServiceChannel"

        val sharedInstance: PixscapeApplication?
            get() = PixscapeApplicationHolder.INSTANCE
    }

    var scapeClient: ScapeClient? = null

    override fun onCreate() {
        super.onCreate()

        PixscapeApplicationHolder.INSTANCE = this

        scapeClient = Scape.scapeClientBuilder
                .withApiKey(BuildConfig.SCAPEKIT_API_KEY)
                .withContext(applicationContext)
                .withDebugSupport(true)
                .build()
        scapeClient?.debugSession?.setLogConfig(LogLevel.LOG_DEBUG, EnumSet.of(LogOutput.CONSOLE))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }
    }

    @RequiresApi(26)
    private fun createNotificationChannel() {
        val notificationChannel = NotificationChannel(CHANNEL_ID,
                                                      "Timer Service Channel",
                                                      NotificationManager.IMPORTANCE_LOW).apply {
            setSound(null, null)
            enableLights(false)
            lightColor = Color.TRANSPARENT
            enableVibration(false)
        }

        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(notificationChannel)
    }
}