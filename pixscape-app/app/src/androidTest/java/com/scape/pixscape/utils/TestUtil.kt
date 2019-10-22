/**
 *
 * Copyright © 2019 Scape Technologies Limited. All rights reserved.
 */
package com.scape.pixscape.utils

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry

object TestUtil {

    const val GPS_LATITUDE_MORDOR = 45.342288
    const val GPS_LONGITUDE_MORDOR = 23.526562

    fun grantAllPermissions() {
        val context: Context = InstrumentationRegistry.getInstrumentation().context

        InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(
            "pm grant " + context.packageName + " android.permission.SYSTEM_ALERT_WINDOW")
        InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(
            "pm grant " + context.packageName + " android.permission.CAMERA")
        InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(
            "pm grant " + context.packageName + " android.permission.ACCESS_FINE_LOCATION")
        InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(
            "pm grant " + context.packageName + " android.permission.ACCESS_COARSE_LOCATION")
        InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(
            "pm grant " + context.packageName + " android.permission.READ_EXTERNAL_STORAGE")
        InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(
            "pm grant " + context.packageName + " android.permission.WRITE_EXTERNAL_STORAGE")
    }
}