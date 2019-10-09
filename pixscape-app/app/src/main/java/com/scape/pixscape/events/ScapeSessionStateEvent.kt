/**
 *
 * Copyright © 2019 Scape Technologies Limited. All rights reserved.
 */
package com.scape.pixscape.events

import com.scape.pixscape.utils.MAX_SCAPE_CONFIDENCE_SCORE
import com.scape.scapekit.ScapeMeasurementsStatus
import com.scape.scapekit.ScapeSessionState

data class ScapeSessionStateEvent(val state: ScapeSessionState = ScapeSessionState.NO_ERROR, val status: ScapeMeasurementsStatus = ScapeMeasurementsStatus.RESULTS_FOUND, val confidenceScore: Double = MAX_SCAPE_CONFIDENCE_SCORE)