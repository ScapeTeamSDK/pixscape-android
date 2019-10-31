/**
 *
 * Copyright © 2019 Scape Technologies Limited. All rights reserved.
 */
package com.scape.pixscape.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.scape.pixscape.R


internal class PreferenceActivity: AppCompatActivity() {

    // region Activity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preference)
    }

    // endregion Activity

}