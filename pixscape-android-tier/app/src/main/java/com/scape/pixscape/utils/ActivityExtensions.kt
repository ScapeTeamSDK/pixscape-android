package com.scape.pixscape.utils

import android.app.Activity
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.view.View
import android.view.Window
import android.view.WindowManager
import com.scape.pixscape.BuildConfig
import com.scape.pixscape.R

fun Activity.hideNavigationStatusBar() {
    window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)

    val decorView = window.decorView

    val uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY

    decorView.systemUiVisibility = uiOptions
}

fun setSystemBarTheme(window: Window, isDark: Boolean) {
    val lFlags = window.decorView.systemUiVisibility
    // Update the SystemUiVisibility dependening on whether we want a Light or Dark theme.
    window.decorView.systemUiVisibility = if (isDark) lFlags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv() else lFlags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
}

fun Activity.setSystemBarTheme(isDark: Boolean) {
    setSystemBarTheme(window, isDark)
}