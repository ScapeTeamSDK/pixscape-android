/**
 *
 * Copyright © 2019 Scape Technologies Limited. All rights reserved.
 */
package com.scape.pixscape.fragments

import com.google.ar.sceneform.ux.ArFragment

/**
 * Custom ArFragment that reverts from nav bar being hidden.
 */
class PixscapeArFragment : ArFragment() {

    override fun onWindowFocusChanged(hasFocus: Boolean) {
    }
}