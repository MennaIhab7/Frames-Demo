package com.example.frames_demo.sentry.frame_calculations;

import org.jetbrains.annotations.NotNull;

class AppFrame implements Comparable<AppFrame> {
    private final long startNanos;
    private final long endNanos;
    private final long durationNanos;
    private final long delayNanos;
    private final boolean isSlow;
    private final boolean isFrozen;

    public long getStartNanos() {
        return startNanos;
    }

    public long getEndNanos() {
        return endNanos;
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

    public long getExpectedDurationNanos() {
        return expectedDurationNanos;
    }

    private final long expectedDurationNanos;

    AppFrame(final long timestampNanos) {
        this(timestampNanos, timestampNanos, 0, 0, false, false, 0);
    }

    AppFrame(
            final long startNanos,
            final long endNanos,
            final long durationNanos,
            final long delayNanos,
            final boolean isSlow,
            final boolean isFrozen,
            final long expectedFrameDurationNanos) {
        this.startNanos = startNanos;
        this.endNanos = endNanos;
        this.durationNanos = durationNanos;
        this.delayNanos = delayNanos;
        this.isSlow = isSlow;
        this.isFrozen = isFrozen;
        this.expectedDurationNanos = expectedFrameDurationNanos;
    }

    @Override
    public int compareTo(final @NotNull AppFrame o) {
        return Long.compare(this.endNanos, o.endNanos);
    }
}


