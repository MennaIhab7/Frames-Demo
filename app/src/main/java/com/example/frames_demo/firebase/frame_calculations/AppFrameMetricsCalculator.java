package com.example.frames_demo.firebase.frame_calculations;

import android.util.Log;
import android.util.SparseIntArray;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.FrameMetricsAggregator;

import com.example.frames_demo.firebase.utils.Constants;

import java.util.HashMap;
import java.util.Map;

public class AppFrameMetricsCalculator {

    private final int normalFrameDuration;

    public AppFrameMetricsCalculator(float refreshRate) {
        this.normalFrameDuration = Math.round(1 / refreshRate * 1000f);
        Log.d("AppFrameMetricsCalculat", "normalFrameDuration: " + normalFrameDuration);
    }

    public static class PerfFrameMetrics {
        int totalFrames;
        int slowFrames;
        int frozenFrames;
        Map<Integer, Integer> slowFramesMap;
        Map<Integer, Integer> frozenFramesMap;

        public PerfFrameMetrics(int totalFrames, int slowFrames, int frozenFrames, Map<Integer, Integer> slowFramesMap, Map<Integer, Integer> frozenFramesMap) {
            this.totalFrames = totalFrames;
            this.slowFrames = slowFrames;
            this.frozenFrames = frozenFrames;
            this.slowFramesMap = slowFramesMap;
            this.frozenFramesMap = frozenFramesMap;
        }

        public Map<Integer, Integer> getSlowFramesMap() {
            return slowFramesMap;
        }

        public Map<Integer, Integer> getFrozenFramesMap() {
            return frozenFramesMap;
        }

        public int getFrozenFrames() {
            return frozenFrames;
        }

        public int getSlowFrames() {
            return slowFrames;
        }

        public int getTotalFrames() {
            return totalFrames;
        }

    }

    /**
     * Calculate total frames, slow frames, and frozen frames from SparseIntArray[] recorded by {@link
     * FrameMetricsAggregator}.
     *
     * @param arr the metrics data collected by {@link FrameMetricsAggregator#getMetrics()}
     * @return the frame metrics
     */
    public @NonNull PerfFrameMetrics calculateFrameMetrics(@Nullable SparseIntArray[] arr) {
        int totalFrames = 0;
        int slowFrames = 0;
        int frozenFrames = 0;
        Map<Integer, Integer> slowFramesMap = new HashMap<>();
        Map<Integer, Integer> frozenFramesMap = new HashMap<>();

        if (arr != null) {
            SparseIntArray frameTimes = arr[FrameMetricsAggregator.TOTAL_INDEX];
            if (frameTimes != null) {
                for (int i = 0; i < frameTimes.size(); i++) {
                    int frameTime = frameTimes.keyAt(i); // duration
                    Log.d("AppFrameMetricsCalculat", "Frame time: " + frameTimes.keyAt(i));
                    int numFrames = frameTimes.valueAt(i); // num of frames with the same duration
                    Log.d("AppFrameMetricsCalculat", "# frames of that time : " + frameTimes.valueAt(i));

                    totalFrames += numFrames;
                    Log.d("AppFrameMetricsCalculat", "totalFrames: " + totalFrames);

                    if (frameTime > Constants.FROZEN_FRAME_TIME) {
                        frozenFrames += numFrames;
                        frozenFramesMap.put(frameTime, numFrames);
                        Log.d("AppFrameMetricsCalculat", "frozen Frames: " + frozenFrames);
                    } else if (frameTime > normalFrameDuration) {
                        slowFrames += numFrames;
                        slowFramesMap.put(frameTime, numFrames);
                        Log.d("AppFrameMetricsCalculat", "slow Frames: " + slowFrames);
                    }
                }
            }
        }
        // Only incrementMetric if corresponding metric is non-zero.
        return new PerfFrameMetrics(totalFrames, slowFrames, frozenFrames, slowFramesMap, frozenFramesMap);
    }
}