package com.scape.pixscape.views

import android.animation.ArgbEvaluator
import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Switch
import androidx.core.content.ContextCompat
import androidx.viewpager.widget.ViewPager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.scape.pixscape.R
import com.scape.pixscape.activities.MainActivity
import com.scape.pixscape.utils.setSystemBarTheme
import kotlinx.android.synthetic.main.view_pager_tabs.view.*

internal class MainTabView : FrameLayout, ViewPager.OnPageChangeListener {

    private lateinit var cameraViewCenterTab: ImageView
    private lateinit var historyViewLeftTab: ImageView
    private lateinit var mapViewRightTab: ImageView
    private lateinit var toggleModeBottomSwitch: Switch

    private lateinit var startTimerButton: FloatingActionButton
    private lateinit var pauseTimerButton: FloatingActionButton
    private lateinit var stopTimerButton: ImageView

    private lateinit var viewTabIndicator: View

    private lateinit var argbEvaluator: ArgbEvaluator

    private var centerColor: Int = 0
    private var sideColor: Int = 0

    private var endViewTranslationX = 0
    private var centerPadding = 0
    private var centerTranslationY = 0

    constructor(context: Context) :
            this(context, null)

    constructor(context: Context, attrs: AttributeSet?) :
            this(context, attrs!!, 0)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) :
            super(context, attrs, defStyleAttr) {
        initialize()
    }

    private fun initialize() {
        val view: View = LayoutInflater.from(context).inflate(R.layout.view_pager_tabs, this, true)

        cameraViewCenterTab = view.findViewById(R.id.view_camera_center)
        historyViewLeftTab = view.findViewById(R.id.view_history_left)
        mapViewRightTab = view.findViewById(R.id.view_map_right)
        toggleModeBottomSwitch = view.findViewById(R.id.view_switch_bottom)

        startTimerButton = view.findViewById(R.id.play_timer_button)
        pauseTimerButton = view.findViewById(R.id.pause_timer_button)
        stopTimerButton  = view.findViewById(R.id.stop_timer_button)

        viewTabIndicator = view.findViewById(R.id.view_tab_indicator)

        //color
        centerColor = ContextCompat.getColor(context, R.color.color_white)
        sideColor = ContextCompat.getColor(context, R.color.color_grey)

        //method to graduate color
        argbEvaluator = ArgbEvaluator()

        //a distance in dp
        centerPadding = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 80F, resources.displayMetrics).toInt()

        // to compute the positions of tabs when swiping right/left
        toggleModeBottomSwitch.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                //position x after transaction finish
                endViewTranslationX = ((view_circle_invisible_bottom.x - historyViewLeftTab.x) - centerPadding).toInt()
                view_circle_invisible_bottom.viewTreeObserver.removeOnGlobalLayoutListener(this)

                centerTranslationY = height - view_circle_invisible_bottom.bottom
            }
        })
    }

    fun setUpWithViewPager(viewPager: ViewPager) {
        viewPager.addOnPageChangeListener(this)

        bindings(viewPager)
    }

    private fun bindings(viewPager: ViewPager) {
        historyViewLeftTab.setOnClickListener {
            if (viewPager.currentItem != 0) {
                viewPager.currentItem = 0
            }
        }

        mapViewRightTab.setOnClickListener {
            if (viewPager.currentItem != 2) {
                viewPager.currentItem = 2
            }
        }

        toggleModeBottomSwitch.setOnCheckedChangeListener { _, isChecked ->
            when(isChecked) {
                true -> {
                    toggleModeBottomSwitch.text = context?.getString(R.string.repeat)

                    startTimerButton.show()
                    time?.visibility = View.VISIBLE
                }
                false -> {
                    toggleModeBottomSwitch.text = context?.getString(R.string.single)

                    startTimerButton.hide()
                    time?.visibility = View.GONE
                }
            }
        }
    }

    private fun setColor(fractionFromCenter: Float) {
        val color: Int = argbEvaluator.evaluate(fractionFromCenter, centerColor, sideColor) as Int

        cameraViewCenterTab.setColorFilter(color)
        mapViewRightTab.setColorFilter(color)
        historyViewLeftTab.setColorFilter(color)
    }

    private fun moveViewTabs(fractionFromCenter: Float) {
        historyViewLeftTab.translationX = fractionFromCenter * endViewTranslationX
        mapViewRightTab.translationX = -fractionFromCenter * endViewTranslationX

        viewTabIndicator.alpha = fractionFromCenter
        viewTabIndicator.scaleX = fractionFromCenter
    }

    private fun moveAndScaleCenter(fractionFromCenter: Float) {
        val scale: Float = .7f + ((1 - fractionFromCenter) * .3f)

        cameraViewCenterTab.scaleX = scale
        cameraViewCenterTab.scaleY = scale

        startTimerButton.scaleX = scale
        startTimerButton.scaleY = scale

        pauseTimerButton.scaleX = scale
        pauseTimerButton.scaleY = scale

        stopTimerButton.scaleX = scale
        stopTimerButton.scaleY = scale

        val translation = fractionFromCenter * centerTranslationY

        cameraViewCenterTab.translationY = translation
        startTimerButton.translationY = translation
        pauseTimerButton.translationY = translation
        stopTimerButton.translationY = translation

        toggleModeBottomSwitch.translationY = translation

        toggleModeBottomSwitch.alpha = (1 - fractionFromCenter)
    }

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
        when (position) {
            0 -> {
                val fractionFromCenter = 1 - positionOffset

                setColor(fractionFromCenter)
                moveViewTabs(fractionFromCenter)
                moveAndScaleCenter(fractionFromCenter)

                viewTabIndicator.translationX = ((positionOffset - 1) * centerPadding)

                (context as MainActivity).setSystemBarTheme(false)
            }
            1 -> {
                setColor(positionOffset)
                moveViewTabs(positionOffset)
                moveAndScaleCenter(positionOffset)

                viewTabIndicator.translationX = (positionOffset * centerPadding)

                (context as MainActivity).setSystemBarTheme(true)
            }
            2 -> {
                (context as MainActivity).setSystemBarTheme(false)
            }
        }
    }

    override fun onPageScrollStateChanged(state: Int) {

    }

    override fun onPageSelected(position: Int) {

    }
}