package org.boofcv.android.fragments

import kotlin.math.max

/**
 * Simplifies computing a moving average of some value
 */
class MovingAverageStats {
    var decayRate = 0.95
    var average = 0.0
    var maximum = 0.0
    var count = 0

    fun update(measurement:Double) {
        if (count == 0) {
            average = measurement
        } else {
            average = average*decayRate + measurement*(1.0-decayRate)
        }
        maximum = max(maximum, measurement)
        count++
    }

    fun reset() {
        average = 0.0
        maximum = 0.0
        count = 0
    }
}