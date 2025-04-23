package com.example.sentrydemo.frame_calculations;

public class AppFrameMetrics {
    private final long startMillis;
    private final long durationNanos;
    private final long delayNanos;
    private final boolean isSlow;
    private final boolean isFrozen;

    public long getStartMillis() {
        return startMillis;
    }

    public long getDurationNanos() {
        return durationNanos;
    }

    public long getDelayNanos() {
        return delayNanos;
    }

    public boolean isSlow() {
        return isSlow;
    }

    public boolean isFrozen() {
        return isFrozen;
    }

    AppFrameMetrics(
            final long startMillis,
            final long durationNanos,
            final long delayNanos,
            final boolean isSlow,
            final boolean isFrozen) {
        this.startMillis = startMillis;
        this.durationNanos = durationNanos;
        this.delayNanos = delayNanos;
        this.isSlow = isSlow;
        this.isFrozen = isFrozen;
    }
}


