package com.example.frames_demo.datadog.frame_calculation

import com.example.frames_demo.datadog.utils.EvictingQueue
import java.util.Queue

internal data class ViewUIPerformanceReport(
    var slowFramesRecords: Queue<SlowFrameRecord> = EvictingQueue(),
    var slowFramesRecordsWithinSpan: Queue<SlowFrameRecord> = EvictingQueue(),

    var slowFramesDurationNs: Long = 0L,
    var ignoredFramesCount: Long = 0L,
    var totalFramesDurationNs: Long = 0L,
    var startTimeMs: Long = 0L,
    var endTimeMs: Long = 0L,
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