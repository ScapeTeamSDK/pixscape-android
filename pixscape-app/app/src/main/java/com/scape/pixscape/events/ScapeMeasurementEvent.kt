/**
 *
 * Copyright © 2019 Scape Technologies Limited. All rights reserved.
 */
package com.scape.pixscape.events

import com.scape.pixscape.models.dto.RouteSection

data class ScapeMeasurementEvent(val scapeLocations: MutableList<RouteSection>)