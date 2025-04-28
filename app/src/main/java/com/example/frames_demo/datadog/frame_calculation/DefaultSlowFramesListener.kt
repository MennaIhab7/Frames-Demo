package com.example.frames_demo.datadog.frame_calculation

import android.os.Build
import android.util.Log
import android.view.Window
import androidx.metrics.performance.FrameData
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min

internal class DefaultSlowFramesListener(
    window: Window,
    private val configuration: SlowFramesConfiguration,
) : FrameStateListener {

    private val expectedDuration = run {
        val refreshRate =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                window.context.display.refreshRate
            else
                window.windowManager.defaultDisplay.refreshRate
        return@run TimeUnit.SECONDS.toNanos(1)/refreshRate
    }
    private var uiPerformanceReport: ViewUIPerformanceReport? = null
    private var startTimeMs: Long = 0

    override fun onStartMonitor(resumed: Boolean) {
        if (!resumed) {
            uiPerformanceReport = null
            startTimeMs = System.currentTimeMillis()
        }
    }

    override fun onStopMonitor(end: Boolean) {
        if (end) {
            uiPerformanceReport?.startTimeMs = startTimeMs
            uiPerformanceReport?.endTimeMs = System.currentTimeMillis()
        }
    }


    // Called from the background thread
    override fun onFrame(volatileFrameData: FrameData) {
        val frameDurationNs = volatileFrameData.frameDurationUiNanos
        val frameStartedTimestampNs = volatileFrameData.frameStartNanos
        val report = getViewPerformanceReport()
        Log.d("onFrame", "frameDurationNs: ${frameDurationNs/1e6}")

        // We have to synchronize here because it's the only way to update
        // all fields of ViewUIPerformanceReport atomically. onFrame is a "hot" method
        // so we can't make ViewUIPerformanceReport immutable because that will force us to
        // create tons of copies on each call which will lead to a lot of gc calls
        synchronized(report) {
            // Updating frames statistics
            report.totalFramesDurationNs += frameDurationNs

            if (frameDurationNs < expectedDuration) {
                report.ignoredFramesCount += 1
                return
            }

            report.slowFramesDurationNs += frameDurationNs

            val previousSlowFrameRecord = report.lastSlowFrameRecord
            val delaySinceLastUpdate = frameStartedTimestampNs -
                    (previousSlowFrameRecord?.startTimestampNs ?: frameStartedTimestampNs)
            if (previousSlowFrameRecord == null ||
                delaySinceLastUpdate > configuration.continuousSlowFrameThresholdNs
            ) {
                // No previous slow frame record or amount of time since the last update
                // is significant enough to consider it idle - adding a new slow frame record.
                if (frameDurationNs > 0) {
                    report.slowFramesRecords += SlowFrameRecord(
                        frameStartedTimestampNs,
                        frameDurationNs
                    )
                }
            } else {
                // It's a continuous slow frame – increasing duration
                previousSlowFrameRecord.durationNs = min(
                    previousSlowFrameRecord.durationNs + frameDurationNs,
                    configuration.maxSlowFrameThresholdNs - 1
                )
            }
        }
    }

    fun getViewPerformanceReport(): ViewUIPerformanceReport {
        if (uiPerformanceReport != null) {
            return uiPerformanceReport!!
        }
        uiPerformanceReport = ViewUIPerformanceReport(
            configuration.maxSlowFramesAmount,
        )
        return uiPerformanceReport!!
    }
}