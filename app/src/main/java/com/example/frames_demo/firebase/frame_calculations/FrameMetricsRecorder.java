package com.example.frames_demo.firebase.frame_calculations;


import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.WindowManager;

import androidx.annotation.VisibleForTesting;
import androidx.core.app.FrameMetricsAggregator;

import com.example.frames_demo.firebase.utils.Optional;


public class FrameMetricsRecorder {

    private final Activity activity;
    private final FrameMetricsAggregator frameMetricsAggregator;

    private final AppFrameMetricsCalculator calculator;

    private boolean isRecording = false;


    /**
     * Creates a recorder for a specific activity.
     *
     * @param activity the activity that the recorder is collecting data from.
     */
    public FrameMetricsRecorder(Activity activity) {
        this(activity, new FrameMetricsAggregator());
    }

    @VisibleForTesting
    FrameMetricsRecorder(
            Activity activity,
            FrameMetricsAggregator frameMetricsAggregator) {
        this.activity = activity;
        this.frameMetricsAggregator = frameMetricsAggregator;
        var display = ((WindowManager) activity.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        this.calculator = new AppFrameMetricsCalculator(display.getRefreshRate());
    }

    /**
     * Starts recording FrameMetrics for the activity window.
     */
    public void start() {
        if (isRecording) {
            Log.v("AppFrameMetricsRecorder",
                    "FrameMetricsAggregator is already recording");
            return;
        }
        frameMetricsAggregator.add(activity);
        isRecording = true;
    }

    /**
     * Stops recording FrameMetrics for the activity window.
     *
     * @return FrameMetrics accumulated during the current recording.
     */
    public Optional<AppFrameMetricsCalculator.PerfFrameMetrics> stop() {
        if (!isRecording) {
            Log.v("AppFrameMetricsRecorder", "Cannot stop because no recording was started");
            return Optional.absent();
        }
        Optional<AppFrameMetricsCalculator.PerfFrameMetrics> data = this.snapshot();
        try {
            // No reliable way to check for hardware-acceleration, so we must catch retroactively (#2736).
            frameMetricsAggregator.remove(activity);
        } catch (IllegalArgumentException | NullPointerException ex) {
            // Both of these exceptions result from android.view.View.addFrameMetricsListener silently
            // failing when the view is not hardware-accelerated. Successful addFrameMetricsListener
            // stores an observer in a list, and initializes the list if it was uninitialized. Invoking
            // View.removeFrameMetricsListener(listener) throws IAE if it doesn't exist in the list, or
            // throws NPE if the list itself was never initialized (#4184).
            if (ex instanceof NullPointerException && Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                // Re-throw above API 28, since the NPE is fixed in API 29:
                // https://android.googlesource.com/platform/frameworks/base/+/140ff5ea8e2d99edc3fbe63a43239e459334c76b
                throw ex;
            }
            Log.v("AppFrameMetricsRecorder",
                    "View not hardware accelerated. Unable to collect FrameMetrics");
            data = Optional.absent();
        }
        frameMetricsAggregator.reset();
        isRecording = false;
        return data;
    }


    /**
     * Snapshots total frames, slow frames, and frozen frames from SparseIntArray[] recorded by {@link
     * FrameMetricsAggregator}.
     *
     * @return {@link AppFrameMetricsCalculator.PerfFrameMetrics} at the time of snapshot.
     */
    private Optional<AppFrameMetricsCalculator.PerfFrameMetrics> snapshot() {
        if (!isRecording) {
            Log.v("AppFrameMetricsRecorder", "No recording has been started.");
            return Optional.absent();
        }
        SparseIntArray[] arr = this.frameMetricsAggregator.getMetrics();
        if (arr == null) {
            Log.v("AppFrameMetricsRecorder", "FrameMetricsAggregator.mMetrics is uninitialized.");
            return Optional.absent();
        }
        SparseIntArray frameTimes = arr[FrameMetricsAggregator.TOTAL_INDEX];
        if (frameTimes == null) {
            Log.v("AppFrameMetricsRecorder", "FrameMetricsAggregator.mMetrics[TOTAL_INDEX] is uninitialized.");
            return Optional.absent();
        }
        return Optional.of(calculator.calculateFrameMetrics(arr));
    }
}

