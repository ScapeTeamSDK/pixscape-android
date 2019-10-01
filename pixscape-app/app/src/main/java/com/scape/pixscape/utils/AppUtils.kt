package com.scape.pixscape.utils

import android.annotation.SuppressLint
import android.content.Context
import android.net.wifi.WifiManager
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayInputStream
import android.telephony.CellSignalStrengthCdma
import android.telephony.CellInfoCdma
import android.telephony.CellSignalStrengthLte
import android.telephony.CellInfoLte
import android.telephony.CellSignalStrengthGsm
import android.telephony.CellInfoGsm
import android.telephony.CellSignalStrengthWcdma
import android.telephony.CellInfoWcdma
import android.telephony.CellInfo
import android.telephony.TelephonyManager


fun downloadKmlFileAsync() = GlobalScope.async {
    val client = OkHttpClient()
    val request = Request.Builder().url("https://scapekit-resources.s3-eu-west-1.amazonaws.com/parking_areas.kml").build()
    val response = client.newCall(request).execute()
    ByteArrayInputStream(response.body?.string()?.toByteArray(Charsets.UTF_8))
}

fun getNetworkStrengthInInt(context: Context): Int {
    val initialTime = System.currentTimeMillis()
    var wifiManager =  (context?.getSystemService(Context.WIFI_SERVICE)) as WifiManager

    var wifiInfo = wifiManager.connectionInfo

//    WifiManager.calculateSignalLevel(wifiInfo.rssi, 5)

    val strentgh = wifiInfo.rssi
    val endTime = System.currentTimeMillis()


    return wifiInfo.rssi
}


@SuppressLint("MissingPermission")
fun getSignalStrength(context: Context): Int {
    val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    var strength: Int = 0
    val cellInfos = telephonyManager.allCellInfo   //This will give info of all sims present inside your mobile
    if (cellInfos != null) {
        for (i in cellInfos.indices) {
            if (cellInfos[i].isRegistered) {
                when {
                    cellInfos[i] is CellInfoWcdma -> {
                        val cellInfoWcdma = cellInfos[i] as CellInfoWcdma
                        val cellSignalStrengthWcdma = cellInfoWcdma.cellSignalStrength
                        strength = cellSignalStrengthWcdma.dbm
                    }
                    cellInfos[i] is CellInfoGsm -> {
                        val cellInfogsm = cellInfos[i] as CellInfoGsm
                        val cellSignalStrengthGsm = cellInfogsm.cellSignalStrength
                        strength = cellSignalStrengthGsm.dbm
                    }
                    cellInfos[i] is CellInfoLte -> {
                        val cellInfoLte = cellInfos[i] as CellInfoLte
                        val cellSignalStrengthLte = cellInfoLte.cellSignalStrength
                        strength = cellSignalStrengthLte.dbm
                    }
                    cellInfos[i] is CellInfoCdma -> {
                        val cellInfoCdma = cellInfos[i] as CellInfoCdma
                        val cellSignalStrengthCdma = cellInfoCdma.cellSignalStrength
                        strength = cellSignalStrengthCdma.dbm
                    }
                }
            }
        }
    }
    return strength
}