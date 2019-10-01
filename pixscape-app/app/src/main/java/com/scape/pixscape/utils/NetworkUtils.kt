/**
 *
 * Copyright © 2019 Scape Technologies Limited. All rights reserved.
 */
package com.scape.pixscape.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.ConnectivityManager.TYPE_MOBILE
import android.net.ConnectivityManager.TYPE_WIFI
import android.net.NetworkInfo
import android.net.wifi.WifiManager

enum class WifiSignalStrength {
    Excellent,
    Good,
    Fair,
    Weak
}


fun getWifiSignalStrength(context: Context): WifiSignalStrength {
    val initialTime = System.currentTimeMillis()
    var wifiManager =  (context.getSystemService(Context.WIFI_SERVICE)) as WifiManager

    var wifiInfo = wifiManager.connectionInfo

//    WifiManager.calculateSignalLevel(wifiInfo.rssi, 5)

    val level = wifiInfo.rssi
    var networkStrength = WifiSignalStrength.Weak

    if (level <= 0 && level >= -50) {
        //Best signal
        networkStrength = WifiSignalStrength.Excellent
    } else if (level < -50 && level >= -60) {
        //Good signal
        networkStrength = WifiSignalStrength.Good
    } else if (level < -60 && level >= -70) {
        //Fair signal
        networkStrength = WifiSignalStrength.Fair
    } else if (level < -70) {
        //Very weak signal
        networkStrength = WifiSignalStrength.Weak
    }

    return networkStrength
}

fun getNetworkignalStrength() {

}

fun checkSignal(context: Context) {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val networkInfo: NetworkInfo? = connectivityManager.activeNetworkInfo

    when (networkInfo?.subtype) {
        TYPE_WIFI -> {

        }
        TYPE_MOBILE -> {

        }
    }
}