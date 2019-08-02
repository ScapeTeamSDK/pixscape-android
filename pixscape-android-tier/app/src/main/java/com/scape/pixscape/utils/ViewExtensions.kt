package com.scape.pixscape.utils

import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.irozon.alertview.AlertView

/** Same as [AlertView.show] but setting immersive mode in the views's window */
fun AlertView.showImmersive(window: Window?, activity: AppCompatActivity) {
    window?.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)

    show(activity)

    window?.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)

    window?.decorView?.postDelayed ({
        window?.decorView?.systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
    }, 1000)
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

