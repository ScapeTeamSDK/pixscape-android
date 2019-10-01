package com.scape.pixscape

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.graphics.Color
import android.os.Build
import androidx.annotation.RequiresApi

class PixscapeApplication: Application() {

    companion object {
        const val CHANNEL_ID = "timerServiceChannel"
    }

    override fun onCreate() {
        super.onCreate()

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