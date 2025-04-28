package com.example.frames_demo.datadog.frame_calculation

data class SlowFramesConfiguration(
    internal val maxSlowFramesAmount: Int = DEFAULT_SLOW_FRAME_RECORDS_MAX_AMOUNT,
    internal val maxSlowFrameThresholdNs: Long = DEFAULT_FROZEN_FRAME_THRESHOLD_NS,
    internal val continuousSlowFrameThresholdNs: Long = DEFAULT_CONTINUOUS_SLOW_FRAME_THRESHOLD_NS,
    internal val freezeDurationThresholdNs: Long = DEFAULT_FREEZE_DURATION_NS,
    internal val minViewLifetimeThresholdNs: Long = DEFAULT_VIEW_LIFETIME_THRESHOLD_NS
) {

    companion object {


        // Taking into account each Hitch takes 64B in the payload, we can have 64KB max per view event
        private const val DEFAULT_SLOW_FRAME_RECORDS_MAX_AMOUNT: Int = 1000
        private const val DEFAULT_CONTINUOUS_SLOW_FRAME_THRESHOLD_NS: Long = 16_666_666L // 1/60 fps in nanoseconds
        private const val DEFAULT_FROZEN_FRAME_THRESHOLD_NS: Long = 700_000_000 // 700ms
        private const val DEFAULT_FREEZE_DURATION_NS: Long = 5_000_000_000L // 5s
        private const val DEFAULT_VIEW_LIFETIME_THRESHOLD_NS: Long = 1_000_000_000L // 1s
    }
}
