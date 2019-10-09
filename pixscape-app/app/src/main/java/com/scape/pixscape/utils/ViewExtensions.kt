package com.scape.pixscape.utils

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.TypedValue
import android.view.*
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.irozon.alertview.AlertView
import android.widget.TextView
import android.view.Gravity
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.google.android.libraries.maps.model.BitmapDescriptor
import com.google.android.libraries.maps.model.BitmapDescriptorFactory

const val SNACKBAR_DURATION_LONG = 4500
const val SNACKBAR_DURATION_SHORT = 3000

/** Same as [AlertView.show] but setting immersive mode in the views's window */
fun AlertView.showImmersive(window: Window?, activity: AppCompatActivity) {
    window?.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)

    show(activity)

    window?.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)

    window?.decorView?.postDelayed ({
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
    }, 1000)
}

fun View.showSnackbar(snackbarText: String, color: Int, timeLength: Int): Snackbar {
    val snackBar = Snackbar.make(this, snackbarText, timeLength)
    val snackbarView = snackBar.view
    snackbarView.setBackgroundColor(ContextCompat.getColor(this.context, color))

    val sbTextView = snackbarView.findViewById(com.google.android.material.R.id.snackbar_text) as TextView
    sbTextView.gravity = Gravity.CENTER_HORIZONTAL or Gravity.CENTER_VERTICAL
    sbTextView.textAlignment = View.TEXT_ALIGNMENT_CENTER

    sbTextView.setMargins(0, 10, 0, 10)

    val params = snackbarView.layoutParams as? FrameLayout.LayoutParams
    if(params != null) {
        params.height = FrameLayout.LayoutParams.WRAP_CONTENT
        snackbarView.layoutParams = params
    }

    snackBar.show()

    return snackBar
}

fun Snackbar.updateSnackBarText(snackbarText: String) {
    val snackbarView = view

    val sbTextView = snackbarView.findViewById(com.google.android.material.R.id.snackbar_text) as TextView
    sbTextView.text = snackbarText
}

fun View.setMargins(leftMarginDp: Int? = null,
                    topMarginDp: Int? = null,
                    rightMarginDp: Int? = null,
                    bottomMarginDp: Int? = null) {
    if (layoutParams is ViewGroup.MarginLayoutParams) {
        val params = layoutParams as ViewGroup.MarginLayoutParams
        leftMarginDp?.run { params.leftMargin = this.dpToPx(context) }
        topMarginDp?.run { params.topMargin = this.dpToPx(context) }
        rightMarginDp?.run { params.rightMargin = this.dpToPx(context) }
        bottomMarginDp?.run { params.bottomMargin = this.dpToPx(context) }

        requestLayout()
    }
}

fun Int.dpToPx(context: Context): Int {
    val metrics = context.resources.displayMetrics
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this.toFloat(), metrics).toInt()
}

fun vectorToBitmap(resources: Resources, resourceId: Int, color: Int) : BitmapDescriptor {
    val vectorDrawable = ResourcesCompat.getDrawable(resources, resourceId, null)
    val bitmap = Bitmap.createBitmap(vectorDrawable!!.intrinsicWidth,
                                     vectorDrawable.intrinsicHeight,
                                     Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    vectorDrawable.setBounds(0, 0, canvas.width, canvas.height)
    DrawableCompat.setTint(vectorDrawable, color)
    vectorDrawable.draw(canvas)
    return BitmapDescriptorFactory.fromBitmap(bitmap)
}

