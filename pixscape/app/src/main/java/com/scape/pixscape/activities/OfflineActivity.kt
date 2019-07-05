/**
 *
 * Copyright © 2019 Scape Technologies Limited. All rights reserved.
 */
package com.scape.pixscape.activities

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.scape.pixscape.R


class OfflineActivity: AppCompatActivity() {

    companion object {
        private const val TAG = "OfflineActivity"
    }

    var receiver: BroadcastReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            Log.d(TAG, "Device is back online")
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.offline_dialog)

        val filter = IntentFilter()
        filter.addAction("com.pixscape.connectivity_on")

        registerReceiver(receiver, filter)
    }

    override fun onDestroy() {
        super.onDestroy()

        unregisterReceiver(receiver)
    }
}