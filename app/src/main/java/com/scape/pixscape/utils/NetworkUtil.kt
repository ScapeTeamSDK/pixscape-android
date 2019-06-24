/**
 *
 * Copyright © 2019 Scape Technologies Limited. All rights reserved.
 */
package com.scape.pixscape.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo

/**
 * Determine the connectivity status
 * https://developer.android.com/training/monitoring-device-state/connectivity-monitoring
 */

object NetworkUtil {

    fun isConnected(context: Context): Boolean {
        val manager: ConnectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork: NetworkInfo? = manager.activeNetworkInfo

        activeNetwork?.let {
            return it.isConnected
        }

        return false
    }
}