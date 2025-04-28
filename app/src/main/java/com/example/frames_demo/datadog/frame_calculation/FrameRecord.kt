package com.example.frames_demo.datadog.frame_calculation

import java.util.concurrent.TimeUnit

internal data class SlowFrameRecord(
    val startTimestampNs: Long,
    var durationNs: Float,
) {
    override fun toString(): String {
        return "${durationNs / NS_IN_MS}ms"
    }

    val startTimestampMs: Long
        get() = System.currentTimeMillis() - TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTimestampNs)

    val isFrozen: Boolean
        get() = durationNs > 700_000_000

    companion object {
        private const val NS_IN_MS = 1_000_000.0
    }
}
