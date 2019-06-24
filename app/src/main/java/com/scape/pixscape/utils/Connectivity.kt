/**
 *
 * Copyright © 2019 Scape Technologies Limited. All rights reserved.
 */
package com.scape.pixscape.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.scape.pixscape.activities.OfflineActivity

class NetworkChangeReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "NetworkChangeReceiver"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        val status: Boolean = NetworkUtil.isConnected(context!!)

        Log.d(TAG, "Connectivity change detected: $status")
        if (!status){
            showOfflineDialogActivity(context)
        }
        else {

            val local = Intent()
            local.action = "com.pixscape.connectivity_on"
            context.sendBroadcast(local)
        }

    }

    private fun showOfflineDialogActivity(context: Context) {
        val intent = Intent(context.applicationContext, OfflineActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_MULTIPLE_TASK
        context.startActivity(intent)
    }
}