package com.scape.pixscape.utils

import android.app.Activity
import android.app.PendingIntent.getActivity
import android.graphics.Color
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.RelativeLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.irozon.alertview.AlertView
import com.scape.pixscape.R
import com.scape.pixscape.activities.MainActivity

/** Same as [AlertView.show] but setting immersive mode in the view's window */
fun AlertView.showImmersive(window: Window?, activity: AppCompatActivity) {
    window?.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)

    show(activity)

    window?.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)

    window?.decorView?.postDelayed ({
        window?.decorView?.systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
    }, 1000)
}

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

fun View.showSnackbar(snackbarText: String, color: Int, timeLength: Int) {
    val snackBar = Snackbar.make(this, snackbarText, timeLength)
    val snackbarView = snackBar.view
    snackbarView.setBackgroundColor(ContextCompat.getColor(this.context, color))

    val params = snackbarView.layoutParams as? FrameLayout.LayoutParams
    params?.bottomMargin = -15

    if(params != null) {
        snackbarView.layoutParams = params
    }

    snackBar.show()
}

