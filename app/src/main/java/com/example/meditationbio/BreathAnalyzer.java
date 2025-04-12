package com.example.meditationbio;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.SystemClock;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class BreathAnalyzer implements SensorEventListener {

    private static final int BUFFER_SIZE = 600; // 20 сек при 30 Гц
    private static final int SMOOTHING_WINDOW = 8;
    private static final int BREATH_MIN = 6;
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

        float z = event.values[2]; // або .values[1] — див. положення телефону
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

        for (int i = 0; i < zValues.size(); i++) {
            int start = Math.max(0, i - SMOOTHING_WINDOW);
            int end = Math.min(zValues.size() - 1, i + SMOOTHING_WINDOW);
            float sum = 0;
            for (int j = start; j <= end; j++) {
                sum += zValues.get(j);
            }
            smooth.add(sum / (end - start + 1));
        }

        // Пошук піків (вдихів)
        int peaks = 0;
        int lastPeak = -50;
        for (int i = 1; i < smooth.size() - 1; i++) {
            if (smooth.get(i) > smooth.get(i - 1) && smooth.get(i) > smooth.get(i + 1)) {
                if (i - lastPeak > 15) {
                    peaks++;
                    lastPeak = i;
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

