package com.example.frames_demo.datadog.frame_calculation

import androidx.metrics.performance.JankStats

internal interface FrameStateListener : JankStats.OnFrameListener {
    fun onStartMonitor(resumed: Boolean) {}
    fun onStopMonitor(end: Boolean) {}
}