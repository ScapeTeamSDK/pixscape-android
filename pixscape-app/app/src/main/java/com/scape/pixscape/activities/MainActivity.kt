package com.scape.pixscape.activities

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.scape.pixscape.R
import com.scape.pixscape.utils.hideNavigationStatusBar

internal class MainActivity : AppCompatActivity() {

    private lateinit var container: FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        container = findViewById(R.id.fragment_main)
    }

    override fun onResume() {
        super.onResume()

        hideNavigationStatusBar()
    }
}
