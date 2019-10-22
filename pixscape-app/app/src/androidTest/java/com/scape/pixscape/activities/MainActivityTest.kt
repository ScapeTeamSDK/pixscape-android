/**
 *
 * Copyright © 2019 Scape Technologies Limited. All rights reserved.
 */
package com.scape.pixscape.activities

import androidx.test.rule.ActivityTestRule
import com.scape.pixscape.R
import com.scape.pixscape.framework.AcceptanceTest
import com.scape.pixscape.utils.TestUtil.grantAllPermissions
import org.junit.Before
import org.junit.Rule
import org.junit.Test

internal class MainActivityTest : AcceptanceTest<MainActivity>(MainActivity::class.java) {

    @JvmField
    @get:Rule
    val activityRule = ActivityTestRule(MainActivity::class.java, false, false)

    @Before
    fun setup() {
        grantAllPermissions()

        // wait for all animations to be completed
        Thread.sleep(2000)
    }


    @Test
    fun appLaunchesSuccessfullyWithDefaultUI() {

        checkThat.viewIsVisible(R.id.card_view_minimap_container)
        checkThat.viewIsVisible(R.id.mini_map_view)

        checkThat.viewIsGone(R.id.pause_timer_button)
        checkThat.viewIsGone(R.id.play_timer_button)
    }

    @Test
    fun onMiniMapPress() {
        // given that the mini map is opened
        events.clickOnView(R.id.mini_map_view)

        // the full map pops up
        checkThat.viewIsDisplayed(R.id.map_mode_switch)
        checkThat.viewIsDisplayed(R.id.full_map_view)

        events.clickOnView(R.id.view_camera_center)
    }

    @Test
    fun onSingleModePress() {
        events.clickOnView(R.id.view_camera_center)

        Thread.sleep(1000)
        checkThat.viewIsGone(R.id.view_switch_bottom)
        checkThat.viewIsGone(R.id.time)
    }

    @Test
    fun onContinuousModeEnabled() {
        events.clickOnView(R.id.view_switch_bottom)

        checkThat.viewIsVisible(R.id.play_timer_button)
        checkThat.viewIsVisible(R.id.view_switch_bottom)
        checkThat.viewIsVisible(R.id.time)
    }

    @Test
    fun onContinuosModePress() {
        events.clickOnView(R.id.view_switch_bottom)
        events.clickOnView(R.id.play_timer_button)

        checkThat.viewIsGone(R.id.view_switch_bottom)

        events.clickOnView(R.id.pause_timer_button)
        events.clickOnView(R.id.stop_timer_button)
    }

    @Test
    fun onViewPagerActionLeftAndRight() {
        events.clickOnView(R.id.view_history_left)

        checkThat.viewIsDisplayed(R.id.traces_list_title)
        checkThat.viewIsVisible(R.id.trace_history_list)

        events.clickOnView(R.id.view_camera_center)

        checkThat.viewIsVisible(R.id.card_view_minimap_container)
        checkThat.viewIsVisible(R.id.mini_map_view)
    }

    @Test
    fun onViewPagerActionLeftAndClickMap() {
        events.clickOnView(R.id.view_history_left)

        checkThat.viewIsDisplayed(R.id.traces_list_title)
        checkThat.viewIsVisible(R.id.trace_history_list)

        events.clickOnView(R.id.view_map_right)

        checkThat.viewIsDisplayed(R.id.full_map_view)

        events.clickOnView(R.id.view_camera_center)
    }

}