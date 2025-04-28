package com.example.frames_demo.datadog.providers

import android.util.Log
import android.view.Window
import androidx.annotation.UiThread
import androidx.metrics.performance.JankStats

interface JankStatsProvider {

    @UiThread
    fun createJankStatsAndTrack(
        window: Window,
        listener: JankStats.OnFrameListener,
    ): JankStats?

    companion object {

        val DEFAULT = object : JankStatsProvider {
            @UiThread
            override fun createJankStatsAndTrack(
                window: Window,
                listener: JankStats.OnFrameListener,
            ): JankStats? {
                return try {
                    JankStats.createAndTrack(window, listener).also {
                        it.jankHeuristicMultiplier = 1f
                    }
                } catch (e: IllegalStateException) {
                    Log.v(
                        "createJankStatsAndTrack",
                        "Unable to attach JankStats to the current window"
                    )
                    null
                }
            }
        }
    }
}