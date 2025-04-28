package com.example.frames_demo.sentry.frame_calculations;

import android.util.Log;

import com.example.frames_demo.sentry.span.ISpan;
import com.example.frames_demo.sentry.span.Span;
import com.example.frames_demo.sentry.utils.Constants;

import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.TimeUnit;

public class ActivityFrameMetricsCollector implements FrameMetricsCollectorListener {
    private final @NotNull ConcurrentSkipListSet<AppFrame> frames = new ConcurrentSkipListSet<>();
    private long lastKnownFrameDurationNanos = 16_666_666L;
    // 30s span duration at 120fps = 3600 frames
    // this is just an upper limit for frames.size, ensuring that the buffer does not
    // grow indefinitely in case of a long running span
    private static final int MAX_FRAMES_COUNT = 3600;
    private static final long ONE_SECOND_NANOS = TimeUnit.SECONDS.toNanos(1);


    private String listenerId;

    private Span span;

    public void startCollection(FrameMetricsCollector frameMetricsCollector) {
        listenerId = frameMetricsCollector.startCollection(this);
        this.span = new Span();
        frames.clear();
    }

    public Span stopCollection(FrameMetricsCollector frameMetricsCollector) {
        if (listenerId != null) {
            frameMetricsCollector.stopCollection(listenerId);
            listenerId = null;
            span.setFinishedDate(System.nanoTime());
            captureFrameMetrics(frameMetricsCollector, span);
            return span;
        }
        return span;
    }

    private void captureFrameMetrics(FrameMetricsCollector frameMetricsCollector, ISpan span) {

        final long spanFinishDate = span.getFinishDate();
        final long spanStartNanos = span.getStartDate();
        final long spanDurationNanos = spanFinishDate - spanStartNanos;
        if (spanDurationNanos <= 0) {
            return;
        }

        final @NotNull AppFramesMetrics framesMetrics = new AppFramesMetrics();

        long frameDurationNanos = lastKnownFrameDurationNanos;

        if (!frames.isEmpty()) {
            // determine relevant start in frames list
            final Iterator<AppFrame> iterator = frames.tailSet(new AppFrame(spanStartNanos)).iterator();

            //noinspection WhileLoopReplaceableByForEach
            while (iterator.hasNext()) {
                final @NotNull AppFrame frame = iterator.next();

                if (frame.getStartNanos() > spanFinishDate) {
                    break;
                }

                if (frame.getStartNanos() >= spanStartNanos && frame.getEndNanos() <= spanFinishDate) {
                    // if the frame is contained within the span, add it 1:1 to the span metrics
                    framesMetrics.addFrame(
                            frame.getStartNanos(),
                            frame.getDurationNanos(),
                            frame.getDelayNanos(),
                            frame.isSlow(),
                            frame.isFrozen()
                    );
                } else if ((spanStartNanos > frame.getStartNanos() && spanStartNanos < frame.getEndNanos())
                        || (spanFinishDate > frame.getStartNanos() && spanFinishDate < frame.getEndNanos())) {
                    // span start or end are within frame
                    // calculate the intersection
                    final long durationBeforeSpan = Math.max(0, spanStartNanos - frame.getStartNanos());
                    final long delayBeforeSpan =
                            Math.max(0, durationBeforeSpan - frame.getExpectedDurationNanos());
                    final long delayWithinSpan =
                            Math.min(frame.getDelayNanos() - delayBeforeSpan, spanDurationNanos);

                    final long frameStart = Math.max(spanStartNanos, frame.getStartNanos());
                    final long frameEnd = Math.min(spanFinishDate, frame.getEndNanos());
                    final long frameDuration = frameEnd - frameStart;
                    framesMetrics.addFrame(
                            frameStart,
                            frameDuration,
                            delayWithinSpan,
                            FrameMetricsCollector.isSlow(frameDuration, frame.getExpectedDurationNanos()),
                            FrameMetricsCollector.isFrozen(frameDuration)
                    );
                }

                frameDurationNanos = frame.getExpectedDurationNanos();
            }
        }

        int totalFrameCount = framesMetrics.getSlowFrozenFrameCount();

        final long nextScheduledFrameNanos = frameMetricsCollector.getLastKnownFrameStartTimeNanos();
        long durationForInterpolate = spanDurationNanos;
        // nextScheduledFrameNanos might be -1 if no frames have been scheduled for drawing yet
        // e.g. can happen during early app start
        if (nextScheduledFrameNanos != -1) {
            // span ends before frame
            var pendingFrames = addPendingFrameDelay(
                    framesMetrics,
                    frameDurationNanos,
                    spanFinishDate,
                    nextScheduledFrameNanos
            );
            totalFrameCount += pendingFrames;
            if (pendingFrames > 0) {
            durationForInterpolate -= Math.max(spanFinishDate - nextScheduledFrameNanos, 0);
            }
        }
        totalFrameCount += interpolateFrameCount(
                framesMetrics,
                frameDurationNanos,
                durationForInterpolate
        );
        final long frameDelayNanos =
                framesMetrics.getSlowFrameDelayNanos() + framesMetrics.getFrozenFrameDelayNanos();
        final double frameDelayInSeconds = frameDelayNanos / 1e9d;
        framesMetrics.setTotalFrameCount(totalFrameCount);

        span.setData(Constants.FRAMES_LIST, framesMetrics.getFrames());
        Log.d("Sentry", "List frames: " + framesMetrics.getFrames());
        span.setData(Constants.FRAMES_TOTAL, framesMetrics.getTotalFrameCount());
        Log.d("Sentry", "Total frames: " + totalFrameCount);
        span.setData(Constants.FRAMES_SLOW, framesMetrics.getSlowFrameCount());
        Log.d("Sentry", "Slow frames: " + framesMetrics.getSlowFrameCount());
        span.setData(Constants.FRAMES_FROZEN, framesMetrics.getFrozenFrameCount());
        Log.d("Sentry", "Frozen frames: " + framesMetrics.getFrozenFrameCount());
        //span.setData(Constants.FRAMES_DELAY, frameDelayInSeconds);
        //Log.d("Sentry", "Frame delay: " + frameDelayInSeconds);
    }


    @Override
    public void onFrameMetricCollected(
            long frameStartNanos,
            long frameEndNanos,
            long durationNanos,
            long delayNanos,
            boolean isSlow,
            boolean isFrozen,
            float refreshRate) {

        // buffer is full, skip adding new frames for now
        // once a span finishes, the buffer will trimmed
        if (frames.size() > MAX_FRAMES_COUNT) {
            return;
        }

        final long expectedFrameDurationNanos =
                (long) ((double) ONE_SECOND_NANOS / (double) refreshRate);
        lastKnownFrameDurationNanos = expectedFrameDurationNanos;

        if (isSlow || isFrozen) {
            frames.add(
                    new AppFrame(
                            frameStartNanos,
                            frameEndNanos,
                            durationNanos,
                            delayNanos,
                            isSlow,
                            isFrozen,
                            expectedFrameDurationNanos));
        }
    }

    private static int interpolateFrameCount(
            final @NotNull AppFramesMetrics framesMetrics,
            final long frameDurationNanos,
            final long spanDurationNanos) {
        // if there are no content changes on Android, also no new frame metrics are provided by the
        // system
        // in order to match the span duration with the total frame count,
        // we simply interpolate the total number of frames based on the span duration
        // this way the data is more sound and we also match the output of the cocoa SDK
        final long frameMetricsDurationNanos = framesMetrics.getTotalDurationNanos();
        final long nonRenderedDuration = spanDurationNanos - frameMetricsDurationNanos;
        if (nonRenderedDuration > 0) {
            return (int) Math.ceil((double) nonRenderedDuration / frameDurationNanos);
        }
        return 0;
    }

    private static int addPendingFrameDelay(
            @NotNull final AppFramesMetrics framesMetrics,
            final long frameDurationNanos,
            final long spanEndNanos,
            final long nextScheduledFrameNanos) {
        final long pendingDurationNanos = Math.max(0, spanEndNanos - nextScheduledFrameNanos);
        final boolean isSlow =
                FrameMetricsCollector.isSlow(pendingDurationNanos, frameDurationNanos);
        if (isSlow) {
            // add a single slow/frozen frame
            final boolean isFrozen = FrameMetricsCollector.isFrozen(pendingDurationNanos);
            final long pendingDelayNanos = Math.max(0, pendingDurationNanos - frameDurationNanos);
            framesMetrics.addFrame(
                    nextScheduledFrameNanos,
                    pendingDurationNanos,
                    pendingDelayNanos,
                    true,
                    isFrozen
            );
            return 1;
        }
        return 0;
    }

}