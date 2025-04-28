package com.example.frames_demo.datadog.frame_calculation

import android.util.Log
import android.view.Window
import androidx.annotation.MainThread
import androidx.metrics.performance.FrameData
import androidx.metrics.performance.JankStats
import com.example.frames_demo.datadog.providers.JankStatsProvider
import java.util.WeakHashMap

internal class FrameStatesAggregator(
    private val frameStateListeners: List<FrameStateListener>,
    private val jankStatsProvider: JankStatsProvider = JankStatsProvider.DEFAULT,
) : JankStats.OnFrameListener {

    private val activeWindowsListener = WeakHashMap<Window, JankStats>()

    @MainThread
    fun startTracking(window: Window) {
        trackWindowJankStats(window)
    }

    @MainThread
    fun pauseTracking(window: Window) {
        activeWindowsListener[window]?.let {
            if (it.isTrackingEnabled) {
                it.isTrackingEnabled = false
            }
        }
        for (i in frameStateListeners.indices) {
            frameStateListeners[i].onStopMonitor(false)
        }
    }

    @MainThread
    fun stopTracking(window: Window) {
        activeWindowsListener.remove(window)?.let {
            it.isTrackingEnabled = false
        }
        for (i in frameStateListeners.indices) {
            frameStateListeners[i].onStopMonitor(true)
        }
    }

    // endregion

    // region JankStats.OnFrameListener

    override fun onFrame(volatileFrameData: FrameData) {
        // This method is called pretty often and forEach{} gonna create iterator instance each time.
        // To reduce gc pressure we use for-loop iteration here:
        volatileFrameData.isJank
        for (i in frameStateListeners.indices) {
            frameStateListeners[i].onFrame(volatileFrameData)
        }
    }

    @MainThread
    private fun trackWindowJankStats(window: Window) {
        val knownJankStats = activeWindowsListener[window]
        for (i in frameStateListeners.indices) {
            frameStateListeners[i].onStartMonitor(knownJankStats != null)
        }
        if (knownJankStats != null) {
            Log.v("trackWindowJankStats", "Resuming jankStats for window $window")
            knownJankStats.isTrackingEnabled = true
        } else {
            Log.v("trackWindowJankStats", "starting jankStats for window ")
        }
        val jankStats = jankStatsProvider.createJankStatsAndTrack(window, this)
        if (jankStats == null) {
            Log.v("trackWindowJankStats", "Unable to create JankStats")
        } else {
            activeWindowsListener[window] = jankStats
        }
    }

    // endregion
}