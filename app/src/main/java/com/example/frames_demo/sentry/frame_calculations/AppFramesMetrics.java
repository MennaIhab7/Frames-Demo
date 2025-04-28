package com.example.frames_demo.sentry.frame_calculations;

import android.util.Log;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

final class AppFramesMetrics {

    private final List<AppFrameMetrics> frames = new LinkedList<>();
    private int totalFrameCount;

    private int slowFrameCount;
    private int frozenFrameCount;

    private long slowFrameDelayNanos;
    private long frozenFrameDelayNanos;

    private long totalDurationNanos;

    public void addFrame(
            final long frameStartNano,
            final long durationNanos,
            final long delayNanos,
            final boolean isSlow,
            final boolean isFrozen) {
        long frameStart = System.currentTimeMillis() - TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - frameStartNano);
        frames.add(new AppFrameMetrics(
                frameStart,
                durationNanos,
                delayNanos,
                isSlow,
                isFrozen
        ));
        totalDurationNanos += durationNanos;
        if (isFrozen) {
            frozenFrameDelayNanos += delayNanos;
            frozenFrameCount += 1;
        } else if (isSlow) {
            slowFrameDelayNanos += delayNanos;
            Log.d("Sentry", "Slow frame:"+slowFrameDelayNanos);
            slowFrameCount += 1;
        }
    }

    public int getSlowFrameCount() {
        return slowFrameCount;
    }

    public int getFrozenFrameCount() {
        return frozenFrameCount;
    }

    public long getSlowFrameDelayNanos() {
        return slowFrameDelayNanos;
    }

    public long getFrozenFrameDelayNanos() {
        return frozenFrameDelayNanos;
    }

    /**
     * Returns the sum of the slow and frozen frames.
     */
    public int getSlowFrozenFrameCount() {
        return slowFrameCount + frozenFrameCount;
    }

    public long getTotalDurationNanos() {
        return totalDurationNanos;
    }

    public List<AppFrameMetrics> getFrames() {
        return Collections.unmodifiableList(frames);
    }

    public int getTotalFrameCount() {
        return totalFrameCount;
    }

    public void setTotalFrameCount(int totalFrameCount) {
        this.totalFrameCount = totalFrameCount;
    }


    public void clear() {
        frames.clear();
        slowFrameCount = 0;
        slowFrameDelayNanos = 0;

        frozenFrameCount = 0;
        frozenFrameDelayNanos = 0;

        totalDurationNanos = 0;
        totalFrameCount = 0;
    }
}

