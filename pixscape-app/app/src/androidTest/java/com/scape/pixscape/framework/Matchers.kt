/**
 *
 * Copyright © 2019 Scape Technologies Limited. All rights reserved.
 */
package com.scape.pixscape.framework

import android.app.Activity
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.espresso.matcher.ViewMatchers.*

/**
 * Pull out Matchers to separate class in Espresso tests.
 */
class Matchers {
    fun <T : Activity> nextOpenActivityIs(clazz: Class<T>) {
        intended(IntentMatchers.hasComponent(clazz.name))
    }

    fun viewIsVisible(@IdRes viewId: Int) {
        onView(withId(viewId)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
    }

    fun viewIsGone(@IdRes viewId: Int) {
        onView(withId(viewId)).check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    fun viewIsInvisible(@IdRes viewId: Int) {
        onView(withId(viewId)).check(matches(withEffectiveVisibility(Visibility.INVISIBLE)))
    }

    fun viewIsDisplayed(@IdRes viewId: Int) {
        onView(withId(viewId)).check(matches(isDisplayed()))
    }

    fun viewIsVisibleAndContainsText(@StringRes stringResource: Int) {
        onView(withText(stringResource)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
    }

    fun viewContainsText(@IdRes viewId: Int, @StringRes stringResource: Int) {
        onView(withId(viewId)).check(matches(withText(stringResource)))
    }
}


