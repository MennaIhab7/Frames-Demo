package com.example.frames_demo.datadog.frame_calculation

import com.example.frames_demo.datadog.utils.EvictingQueue
import java.util.Queue

internal data class ViewUIPerformanceReport(
    var slowFramesRecords: Queue<SlowFrameRecord> = EvictingQueue(),
    var slowFramesCount: Long = 0L,
    var slowFramesDurationNs: Float = 0.0f,
    var ignoredFramesCount: Long = 0L,
    var totalFramesDurationNs: Float = 0.0f,
    var startTimeMs: Long = 0L,
    var endTimeMs: Long = 0L,
    var totalDelayDuration: Float = 0.0f,
) {
    constructor(
        maxSize: Int,
    ) : this(
        slowFramesRecords = EvictingQueue(maxSize),
    )

    val reportDuration: Long
        get() = endTimeMs - startTimeMs

    val lastSlowFrameRecord: SlowFrameRecord?
        get() = slowFramesRecords.lastOrNull()

    val size: Int
        get() = slowFramesRecords.size

    fun isEmpty() = slowFramesRecords.isEmpty()
}