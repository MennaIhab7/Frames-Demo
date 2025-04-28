package com.example.frames_demo.sentry.frame_calculations;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import android.view.Choreographer;
import android.view.FrameMetrics;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;

public class FrameMetricsCollector implements Application.ActivityLifecycleCallbacks {
    private final @NotNull Map<String, FrameMetricsCollectorListener> listenerMap =
            new ConcurrentHashMap<>();
    private @Nullable Handler handler;
    private final @NotNull Set<Window> trackedWindows = new CopyOnWriteArraySet<>();
    private @Nullable WeakReference<Window> currentWindow;
    private @Nullable Window.OnFrameMetricsAvailableListener frameMetricsAvailableListener;

    private static final long oneSecondInNanos = TimeUnit.SECONDS.toNanos(1);
    private static final long frozenFrameThresholdNanos = TimeUnit.MILLISECONDS.toNanos(700);
    private @Nullable Choreographer choreographer;
    private @Nullable Field choreographerLastFrameTimeField;
    private long lastFrameStartNanos = 0;
    private long lastFrameEndNanos = 0;

    @SuppressWarnings("deprecation")
    @SuppressLint({"NewApi", "PrivateApi"})
    public FrameMetricsCollector(final @NotNull Context context) {
        final @NotNull Context appContext = context.getApplicationContext();

        // registerActivityLifecycleCallbacks is only available if Context is an AppContext
        if (!(appContext instanceof Application)) {
            return;
        }
        // FrameMetrics api is only available since sdk version N
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return;
        }
        HandlerThread handlerThread =
                new HandlerThread("another thread");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());

        // We have to register the lifecycle callback, even if no profile is started, otherwise when we
        // start a profile, we wouldn't have the current activity and couldn't get the frameMetrics.
        ((Application) appContext).registerActivityLifecycleCallbacks(this);

        // Most considerations regarding timestamps of frames are inspired from JankStats library:
        // https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:metrics/metrics-performance/src/main/java/androidx/metrics/performance/JankStatsApi24Impl.kt

        // The Choreographer instance must be accessed on the main thread
        new Handler(Looper.getMainLooper())
                .post(
                        () -> {
                            try {
                                choreographer = Choreographer.getInstance();
                            } catch (Throwable e) {
                                Log.v(
                                        " FrameMetricsCollector",
                                        "Error retrieving Choreographer instance. Slow and frozen frames will not be reported.", e);
                            }
                        });
        // Let's get the last frame timestamp from the choreographer private field
        try {
            choreographerLastFrameTimeField = Choreographer.class.getDeclaredField("mLastFrameTimeNanos");
            choreographerLastFrameTimeField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            Log.v(
                    " FrameMetricsCollector", "Unable to get the frame timestamp from the choreographer: ", e);
        }

        frameMetricsAvailableListener =
                (window, frameMetrics, dropCountSinceLastInvocation) -> {
                    final long now = System.nanoTime();
                    final float refreshRate =
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                                    ? window.getContext().getDisplay().getRefreshRate()
                                    : window.getWindowManager().getDefaultDisplay().getRefreshRate();
                    Log.d("FrameMetricsCollector", "refreshRate: " + refreshRate);

                    final long expectedFrameDuration = (long) (oneSecondInNanos / refreshRate);
                    Log.d("FrameMetricsCollector", "expectedFrameDuration: " + oneSecondInNanos);

                    Log.d("FrameMetricsCollector", "expectedFrameDuration: " + expectedFrameDuration);

                    final long cpuDuration = getFrameCpuDuration(frameMetrics);

                    // if totalDurationNanos is smaller than expectedFrameTimeNanos,
                    // it means that the frame was drawn within it's time budget, thus 0 delay
                    final long delayNanos = Math.max(0, cpuDuration - expectedFrameDuration);

                    long startTime = getFrameStartTimestamp(frameMetrics);
                    // If we couldn't get the timestamp through reflection, we use current time
                    if (startTime < 0) {
                        startTime = now - cpuDuration;
                    }
                    // Let's "adjust" the start time of a frame to be after the end of the previous frame
                    startTime = Math.max(startTime, lastFrameEndNanos);
                    // Let's avoid emitting duplicates (start time equals to last start time)
                    if (startTime == lastFrameStartNanos) {
                        return;
                    }
                    lastFrameStartNanos = startTime;
                    lastFrameEndNanos = startTime + cpuDuration;

                    // Most frames take just a few nanoseconds longer than the optimal calculated
                    // duration.
                    // Therefore we subtract one, because otherwise almost all frames would be slow.
                    final boolean isSlow =
                            isSlow(cpuDuration, (long) ((float) oneSecondInNanos / (refreshRate - 1.0f)));


                    final boolean isFrozen = isSlow && isFrozen(cpuDuration);

                    for (FrameMetricsCollectorListener l : listenerMap.values()) {
                        l.onFrameMetricCollected(
                                startTime,
                                lastFrameEndNanos,
                                cpuDuration,
                                delayNanos,
                                isSlow,
                                isFrozen,
                                refreshRate);
                    }
                };
    }


    public @Nullable String startCollection(final @NotNull FrameMetricsCollectorListener listener) {
        final String uid = String.valueOf(UUID.randomUUID());
        listenerMap.put(uid, listener);
        trackCurrentWindow();
        return uid;
    }

    public void stopCollection(String listenerId) {
        listenerMap.remove(listenerId);
        Window window = currentWindow != null ? currentWindow.get() : null;
        if (window != null && listenerMap.isEmpty()) {
            stopTrackingWindow(window);
        }
    }

    private void trackCurrentWindow() {
        Window window = currentWindow != null ? currentWindow.get() : null;
        if (window == null) {
            return;
        }

        if (!trackedWindows.contains(window) && !listenerMap.isEmpty()) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && handler != null) {
                trackedWindows.add(window);
                assert frameMetricsAvailableListener != null;
                addOnFrameMetricsAvailableListener(window, frameMetricsAvailableListener, handler);
            }
        }
    }


    public static boolean isFrozen(long frameDuration) {
        return frameDuration > frozenFrameThresholdNanos;
    }

    public static boolean isSlow(long frameDuration, final long expectedFrameDuration) {
        return frameDuration > expectedFrameDuration;
    }

    /**
     * Return the internal timestamp in the choreographer of the last frame start timestamp through
     * reflection. On Android O the value is read from the frameMetrics itself.
     */
    @SuppressLint("NewApi")
    private long getFrameStartTimestamp(final @NotNull FrameMetrics frameMetrics) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return frameMetrics.getMetric(FrameMetrics.INTENDED_VSYNC_TIMESTAMP);
        }

        return getLastKnownFrameStartTimeNanos();
    }

    public long getLastKnownFrameStartTimeNanos() {
        // Let's read the choreographer private field to get start timestamp of the frame, which
        // uses System.nanoTime() under the hood
        if (choreographer != null && choreographerLastFrameTimeField != null) {
            try {
                Long choreographerFrameStartTime =
                        (Long) choreographerLastFrameTimeField.get(choreographer);
                if (choreographerFrameStartTime != null) {
                    return choreographerFrameStartTime;
                }
            } catch (IllegalAccessException ignored) {
            }
        }
        return -1;
    }

    /**
     * Return time spent on the main thread (cpu) for frame creation. It doesn't consider time spent
     * on the render thread (gpu).
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    private long getFrameCpuDuration(final @NotNull FrameMetrics frameMetrics) {
        Log.d("FrameMetricsCollector", "total = "+frameMetrics.getMetric(FrameMetrics.TOTAL_DURATION)/10e6);
        var f = frameMetrics.getMetric(FrameMetrics.UNKNOWN_DELAY_DURATION)
                + frameMetrics.getMetric(FrameMetrics.INPUT_HANDLING_DURATION)
                + frameMetrics.getMetric(FrameMetrics.ANIMATION_DURATION)
                + frameMetrics.getMetric(FrameMetrics.LAYOUT_MEASURE_DURATION)
                + frameMetrics.getMetric(FrameMetrics.DRAW_DURATION)
                + frameMetrics.getMetric(FrameMetrics.SYNC_DURATION);
        Log.d("FrameMetricsCollector","f= "+f/10e6);
       return f;



        // Inspired by JankStats
        // https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:metrics/metrics-performance/src/main/java/androidx/metrics/performance/JankStatsApi24Impl.kt;l=74-79;drc=1de6215c6bd9e887e3d94556e9ac55cfb7b8c797
//        return frameMetrics.getMetric(FrameMetrics.UNKNOWN_DELAY_DURATION)
//                + frameMetrics.getMetric(FrameMetrics.INPUT_HANDLING_DURATION)
//                + frameMetrics.getMetric(FrameMetrics.ANIMATION_DURATION)
//                + frameMetrics.getMetric(FrameMetrics.LAYOUT_MEASURE_DURATION)
//                + frameMetrics.getMetric(FrameMetrics.DRAW_DURATION)
//                + frameMetrics.getMetric(FrameMetrics.SYNC_DURATION);
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @androidx.annotation.Nullable Bundle savedInstanceState) {

    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        setCurrentWindow(activity.getWindow());


    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {

    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {

    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
        stopTrackingWindow(activity.getWindow());
        if (currentWindow != null && currentWindow.get() == activity.getWindow()) {
            currentWindow = null;
        }
    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {

    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {

    }

    private void setCurrentWindow(final @NotNull Window window) {
        if (currentWindow != null && currentWindow.get() == window) {
            return;
        }
        currentWindow = new WeakReference<>(window);
        trackCurrentWindow();
    }

    @SuppressLint("NewApi")
    private void stopTrackingWindow(final @NotNull Window window) {
        if (trackedWindows.contains(window)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                try {
                    removeOnFrameMetricsAvailableListener(
                            window, frameMetricsAvailableListener);
                } catch (Exception e) {
                    Log.v(
                            " FrameMetricsCollector", "Failed to remove frameMetricsAvailableListener", e);
                }
            }
            trackedWindows.remove(window);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void addOnFrameMetricsAvailableListener(
            final @NotNull Window window,
            final @NotNull Window.OnFrameMetricsAvailableListener frameMetricsAvailableListener,
            final @Nullable Handler handler) {
        window.addOnFrameMetricsAvailableListener(frameMetricsAvailableListener, handler);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void removeOnFrameMetricsAvailableListener(
            final @NotNull Window window,
            final @Nullable Window.OnFrameMetricsAvailableListener frameMetricsAvailableListener) {
        window.removeOnFrameMetricsAvailableListener(frameMetricsAvailableListener);
    }
}
