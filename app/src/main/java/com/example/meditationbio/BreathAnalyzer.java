package com.example.meditationbio;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.SystemClock;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class BreathAnalyzer implements SensorEventListener {

    private static final int BUFFER_SIZE = 1200; // ~20 сек при 60 Гц (типово для акселерометра)
    private static final int SMOOTHING_WINDOW = 20;
    private static final int MIN_PEAK_DISTANCE = 30;
    private static final float MIN_PEAK_AMPLITUDE = 0.007f;

    private static final int BREATH_MIN = 5;
    private static final int BREATH_MAX = 40;

    private final List<Float> zValues = new ArrayList<>();
    private final BreathListener listener;
    private long startTime;
    private boolean done = false;

    public BreathAnalyzer(BreathListener listener) {
        this.listener = listener;
        this.startTime = SystemClock.elapsedRealtime();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (done) return;

        float z = event.values[2]; // вимір руху по вертикалі (коли лежить на животі)
        zValues.add(z);

        if (zValues.size() > BUFFER_SIZE) {
            zValues.remove(0);
        }

        if (zValues.size() == BUFFER_SIZE) {
            analyze();
            done = true;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    private void analyze() {
        List<Float> smooth = new ArrayList<>();

        // Ковзне середнє згладжування
        for (int i = 0; i < zValues.size(); i++) {
            int start = Math.max(0, i - SMOOTHING_WINDOW);
            int end = Math.min(zValues.size() - 1, i + SMOOTHING_WINDOW);
            float sum = 0;
            for (int j = start; j <= end; j++) {
                sum += zValues.get(j);
            }
            smooth.add(sum / (end - start + 1));
        }

        // Пошук піків з фільтрацією по амплітуді
        int peaks = 0;
        int lastPeak = -MIN_PEAK_DISTANCE;
        for (int i = 1; i < smooth.size() - 1; i++) {
            float current = smooth.get(i);
            float prev = smooth.get(i - 1);
            float next = smooth.get(i + 1);

            if (current > prev && current > next) {
                if ((i - lastPeak) > MIN_PEAK_DISTANCE) {
                    float localMin = Math.min(prev, next);
                    float amplitude = current - localMin;

                    if (amplitude >= MIN_PEAK_AMPLITUDE) {
                        peaks++;
                        lastPeak = i;
                    }
                }
            }
        }

        int brpm = (int) ((peaks * 60.0) / (BUFFER_SIZE / 30.0));

        if (brpm < BREATH_MIN || brpm > BREATH_MAX) {
            Log.w("BREATH", "BRPM out of range: " + brpm);
            return;
        }

        Log.d("BREATH", "Final BRPM: " + brpm);
        if (listener != null) {
            listener.onBreathRateDetected(brpm);
        }
    }

    public void reset() {
        zValues.clear();
        done = false;
        startTime = SystemClock.elapsedRealtime();
    }
}

interface BreathListener {
    void onBreathRateDetected(int brpm);
}