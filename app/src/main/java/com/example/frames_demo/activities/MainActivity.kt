package com.example.frames_demo.activities

import android.animation.AnimatorListenerAdapter
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.frames_demo.R
import com.example.frames_demo.datadog.frame_calculation.DefaultSlowFramesListener
import com.example.frames_demo.datadog.frame_calculation.FrameStatesAggregator
import com.example.frames_demo.datadog.frame_calculation.SlowFramesConfiguration
import com.example.frames_demo.firebase.frame_calculations.AppFrameMetricsCalculator.PerfFrameMetrics
import com.example.frames_demo.firebase.frame_calculations.FrameMetricsRecorder
import com.example.frames_demo.firebase.utils.Optional
import com.example.frames_demo.sentry.frame_calculations.ActivityFrameMetricsCollector
import com.example.frames_demo.sentry.frame_calculations.AppFrameMetrics
import com.example.frames_demo.sentry.frame_calculations.FrameMetricsCollector
import com.example.frames_demo.sentry.span.Span
import com.example.frames_demo.sentry.utils.Constants
import java.util.Date

class MainActivity : AppCompatActivity() {
    private val activityFrameMetricsCollector: ActivityFrameMetricsCollector =
        ActivityFrameMetricsCollector()
    private var frameMetricsCollector: FrameMetricsCollector? = null
    private lateinit var frameStatesAggregator: FrameStatesAggregator
    private lateinit var slowFramesListener: DefaultSlowFramesListener
    private val slowFramesConfiguration = SlowFramesConfiguration()
    private var isPrevSentryStopped = true
    private var isPrevFirebaseStopped = true
    private var isPrevDatadogStopped = true


    /**
     * @noinspection unchecked
     */
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        this.enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        frameMetricsCollector = FrameMetricsCollector(applicationContext)
        val frameMetricsRecorder = FrameMetricsRecorder(this)
        slowFramesListener = DefaultSlowFramesListener(window, slowFramesConfiguration)
        frameStatesAggregator = FrameStatesAggregator(listOf(slowFramesListener))

        val resultsText: TextView =
            findViewById(R.id.text_view)

        findViewById<View>(R.id.start_sentry_btn).setOnClickListener {
            if (isPrevSentryStopped) {
                activityFrameMetricsCollector.startCollection(frameMetricsCollector)
            }
            isPrevSentryStopped = false
        }

        findViewById<View>(R.id.stop_sentry_btn).setOnClickListener {
            isPrevSentryStopped = true
            showSentryResults(resultsText)
        }
        findViewById<View>(R.id.start_firebase_btn).setOnClickListener {
            if(isPrevFirebaseStopped) {
            frameMetricsRecorder.start() }
            isPrevFirebaseStopped = false
        }

        findViewById<View>(R.id.stop_firebase_btn).setOnClickListener {
            isPrevFirebaseStopped = true
            val optionalFrameMetrics: Optional<PerfFrameMetrics> =
                frameMetricsRecorder.stop()
            showFirebaseResults(resultsText, optionalFrameMetrics)
        }
        findViewById<View>(R.id.start_datadog_btn).setOnClickListener {
            if(isPrevDatadogStopped) {
                frameStatesAggregator.startTracking(
                    window
                )
            }
            isPrevDatadogStopped = false
        }

        findViewById<View>(R.id.stop_datadog_btn).setOnClickListener {
            isPrevDatadogStopped = true
            frameStatesAggregator.stopTracking(window)
            showDatadogResults(resultsText)
        }


        findViewById<View>(R.id.start_computation_btn).setOnClickListener { performHeavyComputation() }
        findViewById<View>(R.id.start_animation_btn).setOnClickListener { v: View ->
            v.animate()
                .translationXBy(200f) // Move 200 pixels to the right
                .rotation(360f) // Rotate 360 degrees
                .scaleX(1.5f) // Scale X by 1.5
                .scaleY(1.5f) // Scale Y by 1.5
                .alpha(0.5f) // Fade out to 50% alpha
                .setDuration(1000) // Animation duration in milliseconds
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: android.animation.Animator) {
                        // Optional: Code to run when the animation finishes
                        v.alpha = 1f // Reset alpha
                        v.scaleX = 1f
                        v.scaleY = 1f
                        v.translationX = 0f
                        v.rotation = 0f
                    }
                })
                .start()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)
        ) { v: View, insets: WindowInsetsCompat ->
            val systemBars: androidx.core.graphics.Insets =
                insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun showSentryResults(resultsText: TextView) {
        val span: Span =
            activityFrameMetricsCollector.stopCollection(frameMetricsCollector)
                ?: return

        val data = span.data
        resultsText.text = ""
        val frames: List<AppFrameMetrics>? =
            span.getData(Constants.FRAMES_LIST) as List<AppFrameMetrics>?
        for ((key, value) in data) {
            if (key != Constants.FRAMES_LIST) {
                resultsText.append("\n$key: $value")
            }
        }
        resultsText.append("\n\nFrames")
        var i = 0
        checkNotNull(frames)
        for (frame in frames) {
            i++
            resultsText.append("\nFrame $i")
            resultsText.append("\nis slow: ${frame.isSlow}")
            resultsText.append("\nis frozen: ${frame.isFrozen}")
            resultsText.append("\nDuration: ${frame.durationNanos / 1e6} ms")
            resultsText.append("\nStart time: ${Date(frame.startMillis)}")
        }
    }

    private fun showFirebaseResults(
        resultsText: TextView,
        optionalFrameMetrics: Optional<PerfFrameMetrics>
    ) {
        resultsText.text = ""
        if (optionalFrameMetrics.isAvailable) {
            val frameMetrics: PerfFrameMetrics? = optionalFrameMetrics.get()
            val slowFramesMap: Map<Int, Int> = frameMetrics!!.slowFramesMap
            val frozenFramesMap: Map<Int, Int> = frameMetrics.frozenFramesMap

            resultsText.append(
                "\nSlow frames: ${frameMetrics.slowFrames}"
            )
            resultsText.append(
                "\n\nFrozen frames: ${frameMetrics.frozenFrames}"
            )
            resultsText.append(
                "\nTotal frames: ${frameMetrics.totalFrames}"
            )
            for ((key, value) in slowFramesMap) {
                resultsText.append(
                    "\n Slow frames that took $key ms are ${value} frames"
                )
            }
            for ((key, value) in frozenFramesMap) {
                resultsText.append(
                    " \n Frozen frames that took $key ms are ${value} frames"
                )
            }
        }
    }
    private fun showDatadogResults(
        resultsText: TextView,
    ) {
        resultsText.text = ""
        resultsText.append("Total slow frames duration: ${slowFramesListener.getViewPerformanceReport().slowFramesDurationNs/ 1e6}")
        resultsText.append("\ntotal frames duration that comes: ${slowFramesListener.getViewPerformanceReport().totalFramesDurationNs/ 1e6}")
        resultsText.append("\nSpan duration: ${slowFramesListener.getViewPerformanceReport().reportDuration}")

        val slowFrameRecords = slowFramesListener.getViewPerformanceReport().slowFramesRecords
        var i = 0
        while (!slowFrameRecords.isEmpty()) {
            i++
            val slowFrameRecord = slowFrameRecords.remove()
            if (slowFrameRecord != null) {
                if (slowFrameRecord.isFrozen) {
                    resultsText.append("\nFrozen Frame $i :")
                } else {
                    resultsText.append("\nSlow Frame $i :")
                }
            }
            if (slowFrameRecord != null) {
                resultsText.append("\nstartTime: ${Date(slowFrameRecord.startTimestampMs)}")
                resultsText.append("\nduration: ${slowFrameRecord.durationNs / 1e6}")

            }
        }
    }


    private fun performHeavyComputation() {
        val startTimeLoop = System.nanoTime()
        val loopResult = cpuBoundLoop()
        val endTimeLoop = System.nanoTime()
        System.out.printf(
            "CPU-Bound Loop Result: %d (Time: %.3f ms)\n",
            loopResult, (endTimeLoop - startTimeLoop) / 1000000.0
        )
        println("Heavy computation finished.")
    }

    private fun cpuBoundLoop(): Long {
        var result: Long = 0
        for (i in 0..1) {
            result += i * i
            try {
                Thread.sleep(800)
            } catch (e: InterruptedException) {
                throw RuntimeException(e)
            }
        }
        return result
    }


    override fun onStop() {
        super.onStop()
        activityFrameMetricsCollector.stopCollection(frameMetricsCollector)
    }
}
