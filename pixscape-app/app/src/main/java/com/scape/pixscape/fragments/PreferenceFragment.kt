/**
 *
 * Copyright © 2019 Scape Technologies Limited. All rights reserved.
 */
package com.scape.pixscape.fragments

import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.scape.pixscape.BuildConfig
import com.scape.pixscape.R


class PreferenceFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.app_preferences, rootKey)

        val versionPreference: Preference? = findPreference(getString(R.string.version_key))

        versionPreference.apply {
            this?.summary = BuildConfig.VERSION_NAME + "." + BuildConfig.VERSION_CODE
        }
    }
}