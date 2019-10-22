package com.scape.pixscape.activities


import android.view.View
import android.view.ViewGroup
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnit4
import com.scape.pixscape.R
import com.scape.pixscape.utils.TestUtil
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.hamcrest.TypeSafeMatcher
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
internal class MainScreenMiniMapMainFullMap {

    @Rule
    @JvmField
    var mActivityTestRule = ActivityTestRule(MainActivity::class.java)

    @Before
    fun setup() {
        TestUtil.grantAllPermissions()
    }

    @Test
    fun mainScreenMiniMapMainFullMap() {
        val cardView = onView(
            allOf(
                withId(R.id.card_view_minimap_container),
                childAtPosition(
                    allOf(
                        withId(R.id.camera_container),
                        childAtPosition(
                            withId(R.id.fragment_main),
                            0
                        )
                    ),
                    0
                ),
                isDisplayed()
            )
        )
        cardView.perform(click())

        val appCompatImageView = onView(
            allOf(
                withId(R.id.view_camera_center),
                childAtPosition(
                    allOf(
                        withId(R.id.view_pager_container),
                        childAtPosition(
                            withId(R.id.main_tab_view),
                            0
                        )
                    ),
                    2
                ),
                isDisplayed()
            )
        )
        appCompatImageView.perform(click())

        val appCompatImageView2 = onView(
            allOf(
                withId(R.id.view_map_right),
                childAtPosition(
                    allOf(
                        withId(R.id.view_pager_container),
                        childAtPosition(
                            withId(R.id.main_tab_view),
                            0
                        )
                    ),
                    7
                ),
                isDisplayed()
            )
        )
        appCompatImageView2.perform(click())
    }

    private fun childAtPosition(
        parentMatcher: Matcher<View>, position: Int
    ): Matcher<View> {

        return object : TypeSafeMatcher<View>() {
            override fun describeTo(description: Description) {
                description.appendText("Child at position $position in parent ")
                parentMatcher.describeTo(description)
            }

            public override fun matchesSafely(view: View): Boolean {
                val parent = view.parent
                return parent is ViewGroup && parentMatcher.matches(parent)
                        && view == parent.getChildAt(position)
            }
        }
    }
}
