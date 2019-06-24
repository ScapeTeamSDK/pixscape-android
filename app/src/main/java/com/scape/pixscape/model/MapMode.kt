/**
 *
 * Copyright © 2019 Scape Technologies Limited. All rights reserved.
 */
package com.scape.pixscape.model

class MapMode {

    var mapMode = com.scape.pixscape.model.MapMode()

    enum class MapMode(val mode: String) {
        Standard(""),
        Hybrid(""),
        Satellite("")
    }
}