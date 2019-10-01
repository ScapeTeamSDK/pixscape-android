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
import java.util.*

enum class NetworkSignalStrength {
    Excellent,
    Good,
    Fair,
    Weak,
    Unavailable
}

enum class ConnectionType {
    Wifi,
    Mobile
}

fun getWifiSignalStrength(context: Context):  NetworkSignalStrength {
    val wifiManager =  (context.getSystemService(Context.WIFI_SERVICE)) as WifiManager

    val wifiInfo = wifiManager.connectionInfo

    val level = wifiInfo.rssi
    var networkStrength = NetworkSignalStrength.Unavailable

    if (level <= 0 && level >= -50) {
        //Best signal
        networkStrength = NetworkSignalStrength.Excellent
    } else if (level < -50 && level >= -60) {
        //Good signal
        networkStrength = NetworkSignalStrength.Good
    } else if (level < -60 && level >= -70) {
        //Fair signal
        networkStrength = NetworkSignalStrength.Fair
    } else if (level < -70) {
        //Very weak signal
        networkStrength = NetworkSignalStrength.Weak
    }

    return networkStrength
}

fun checkConnectionType(context: Context): EnumSet<ConnectionType> {
    val connectivityManager = context!!.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    var isWifiConn = false
    var isMobileConn = false
    connectivityManager.allNetworks.forEach { network ->
        connectivityManager.getNetworkInfo(network).apply {
            when (type) {
                TYPE_WIFI -> {
                    isWifiConn = isWifiConn or isConnected
                }
                TYPE_MOBILE -> {
                    isMobileConn = isMobileConn or isConnected
                }
            }
        }
    }

    if (isWifiConn && isMobileConn)
        return EnumSet.of(ConnectionType.Wifi, ConnectionType.Mobile)

    if (isWifiConn)
        return EnumSet.of(ConnectionType.Wifi)

    if (isMobileConn)
        return EnumSet.of(ConnectionType.Mobile)

    return EnumSet.of(null)
}

fun checkNetworkSignalStrength(context: Context): NetworkSignalStrength {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val networkInfo: NetworkInfo = connectivityManager.activeNetworkInfo ?: return NetworkSignalStrength.Weak

    return when (networkInfo.subtype) {
        TYPE_WIFI -> {
            getWifiSignalStrength(context)

        }
        TYPE_MOBILE -> {
            NetworkSignalStrength.Unavailable
        }

        else -> NetworkSignalStrength.Unavailable
    }
}