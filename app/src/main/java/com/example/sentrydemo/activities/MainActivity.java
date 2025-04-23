package com.example.sentrydemo.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.sentrydemo.frame_calculations.ActivityFrameMetricsCollector;
import com.example.sentrydemo.frame_calculations.AppFrameMetrics;
import com.example.sentrydemo.frame_calculations.FrameMetricsCollector;
import com.example.sentrydemo.R;
import com.example.sentrydemo.span.Span;
import com.example.sentrydemo.utils.Constants;

import java.util.Date;
import java.util.List;
import java.util.Map;


public class MainActivity extends AppCompatActivity {

    private final ActivityFrameMetricsCollector activityFrameMetricsCollector = new ActivityFrameMetricsCollector();
    private FrameMetricsCollector frameMetricsCollector = null;
    private boolean isPrevSpanStopped = true;


    /**
     * @noinspection unchecked
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        frameMetricsCollector = new FrameMetricsCollector(getApplicationContext());
        TextView resultsText = findViewById(R.id.text_view);

        findViewById(R.id.start_span_btn).setOnClickListener(v -> {
            if (isPrevSpanStopped) {
                activityFrameMetricsCollector.startCollection(frameMetricsCollector);
            }
            isPrevSpanStopped = false;
        });

        findViewById(R.id.stop_span_btn).setOnClickListener(v -> {
            isPrevSpanStopped = true;
            Span span = activityFrameMetricsCollector.stopCollection(frameMetricsCollector);
            if (span == null) return;

            Map<String, Object> data = span.getData();
            resultsText.setText("");
            List<AppFrameMetrics> frames = (List<AppFrameMetrics>) span.getData(Constants.FRAMES_LIST);
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                if (!entry.getKey().equals(Constants.FRAMES_LIST)) {
                    resultsText.append("\n" + entry.getKey() + ": " + entry.getValue().toString());
                }
            }
            resultsText.append("\n\nFrames \n");
            int i = 0;
            assert frames != null;
            for (AppFrameMetrics frame : frames) {
                i++;
                resultsText.append("\nFrame " + i);
                resultsText.append("\nis slow: " + frame.isSlow());
                resultsText.append("\nis frozen: " + frame.isFrozen());
                resultsText.append("\nDuration: " + frame.getDurationNanos() / 1e6 + " ms");
                resultsText.append("\nStart time: " + new Date(frame.getStartMillis()));
            }
        });

        findViewById(R.id.start_computation_btn).setOnClickListener(v -> performHeavyComputation());
        findViewById(R.id.start_animation_btn).setOnClickListener(v -> v.animate()
                .translationXBy(200f) // Move 200 pixels to the right
                .rotation(360f)      // Rotate 360 degrees
                .scaleX(1.5f)        // Scale X by 1.5
                .scaleY(1.5f)        // Scale Y by 1.5
                .alpha(0.5f)         // Fade out to 50% alpha
                .setDuration(1000)   // Animation duration in milliseconds
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        // Optional: Code to run when the animation finishes
                        v.setAlpha(1f); // Reset alpha
                        v.setScaleX(1f);
                        v.setScaleY(1f);
                        v.setTranslationX(0f);
                        v.setRotation(0f);
                    }
                })
                .start());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void performHeavyComputation() {
        long startTimeLoop = System.nanoTime();
        long loopResult = cpuBoundLoop();
        long endTimeLoop = System.nanoTime();
        System.out.printf("CPU-Bound Loop Result: %d (Time: %.3f ms)\n",
                loopResult, (endTimeLoop - startTimeLoop) / 1_000_000.0);
        System.out.println("Heavy computation finished.");
    }

    private long cpuBoundLoop() {
        long result = 0;
        for (int i = 0; i < 1000000000L; i++) {
            result += (long) i * i;
        }
        return result;
    }


    @Override
    protected void onStop() {
        super.onStop();
        activityFrameMetricsCollector.stopCollection(frameMetricsCollector);
    }
}
