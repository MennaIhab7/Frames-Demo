package com.example.frames_demo.sentry.frame_calculations;

public interface FrameMetricsCollectorListener {
    /**
     * Called when a frame is collected.
     *
     * @param frameStartNanos Start timestamp of a frame in nanoseconds relative to
     *                        System.nano time().
     * @param frameEndNanos   End timestamp of a frame in nanoseconds relative to System.nano time().
     * @param durationNanos   Duration in nanoseconds of the time spent from the cpu on the main
     *                        thread to create the frame.
     * @param delayNanos      the frame delay, in nanoseconds.
     * @param isSlow          True if the frame is considered slow, rendering taking longer than the
     *                        refresh-rate based budget, false otherwise.
     * @param isFrozen        True if the frame is considered frozen, rendering taking longer than 700ms,
     *                        false otherwise.
     * @param refreshRate     the last known refresh rate when the frame was rendered.
     */
    void onFrameMetricCollected(
            final long frameStartNanos,
            final long frameEndNanos,
            final long durationNanos,
            final long delayNanos,
            final boolean isSlow,
            final boolean isFrozen,
            final float refreshRate
    );
}

