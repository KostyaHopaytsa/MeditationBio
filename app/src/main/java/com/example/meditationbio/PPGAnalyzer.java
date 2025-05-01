package com.example.meditationbio;

import android.graphics.ImageFormat;
import android.media.Image;
import android.os.SystemClock;
import android.util.Log;

import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Queue;

@ExperimentalGetImage
public class PPGAnalyzer implements ImageAnalysis.Analyzer {

    private static final int MAX_BUFFER_SIZE = 600; // ~20 секунд при 30 FPS
    private static final int STABILIZATION_TIME_MS = 2500;
    private static final int BPM_MIN = 40;
    private static final int BPM_MAX = 150;

    private final Queue<Double> brightnessValues = new LinkedList<>();
    private final BPMListener bpmListener;

    private boolean stabilized = false;
    private boolean done = false;
    private long startTime = 0;

    public PPGAnalyzer(BPMListener bpmListener) {
        this.bpmListener = bpmListener;
        this.startTime = SystemClock.elapsedRealtime();
    }

    @Override
    public void analyze(ImageProxy image) {
        if (done) {
            image.close();
            return;
        }

        long now = SystemClock.elapsedRealtime();
        if (!stabilized && now - startTime >= STABILIZATION_TIME_MS) {
            stabilized = true;
        }

        if (!stabilized) {
            image.close();
            return;
        }

        Image img = image.getImage();
        if (img != null && img.getFormat() == ImageFormat.YUV_420_888) {
            ByteBuffer buffer = img.getPlanes()[0].getBuffer();
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);

            long sum = 0;
            for (byte b : data) {
                sum += (b & 0xFF);
            }

            double average = sum / (double) data.length;

            brightnessValues.add(average);
            if (brightnessValues.size() > MAX_BUFFER_SIZE) {
                brightnessValues.poll();
            }

            if (brightnessValues.size() == MAX_BUFFER_SIZE) {
                computeFinalBPM();
                done = true;
            }
        }

        image.close();
    }

    private void computeFinalBPM() {
        Double[] raw = brightnessValues.toArray(new Double[0]);
        double[] signal = new double[raw.length];

        // 1. EMA згладжування
        double alpha = 0.1;
        signal[0] = raw[0];
        for (int i = 1; i < raw.length; i++) {
            signal[i] = alpha * raw[i] + (1 - alpha) * signal[i - 1];
        }

        // 2. Нормалізація
        double mean = 0;
        for (double v : signal) mean += v;
        mean /= signal.length;
        for (int i = 0; i < signal.length; i++) {
            signal[i] -= mean;
        }

        // 3. Bandpass-фільтрація (грубе виділення коливань)
        double[] bandpassed = new double[signal.length];
        for (int i = 1; i < signal.length - 1; i++) {
            bandpassed[i] = signal[i] - 0.5 * (signal[i - 1] + signal[i + 1]);
        }

        // 4. Пошук піків
        int peaks = 0;
        int minPeakDistance = 6; // зменшено з 10 для більшої чутливості
        double threshold = calculateDynamicThreshold(bandpassed, 0.8); // зменшено з 1.2

        for (int i = minPeakDistance; i < bandpassed.length - minPeakDistance; i++) {
            boolean isPeak = bandpassed[i] > threshold;
            for (int j = 1; j <= minPeakDistance; j++) {
                if (bandpassed[i] <= bandpassed[i - j] || bandpassed[i] <= bandpassed[i + j]) {
                    isPeak = false;
                    break;
                }
            }
            if (isPeak) {
                peaks++;
                i += minPeakDistance;
            }
        }

        // 5. Розрахунок BPM
        double durationSec = MAX_BUFFER_SIZE / 30.0;
        double bpm = (double) peaks * 60 / durationSec;

        if (bpm < BPM_MIN || bpm > BPM_MAX) {
            Log.w("PPG", "BPM out of range: " + bpm);
            if (bpmListener != null) bpmListener.onBpmInvalid();
            return;
        }

        int roundedBpm = (int) bpm;
        Log.d("PPG", "Final BPM: " + roundedBpm);
        if (bpmListener != null) {
            bpmListener.onBpmDetected(roundedBpm);
        }
    }

    private double calculateDynamicThreshold(double[] signal, double multiplier) {
        double mean = 0;
        for (double v : signal) mean += v;
        mean /= signal.length;

        double std = 0;
        for (double v : signal) std += Math.pow(v - mean, 2);
        std = Math.sqrt(std / signal.length);

        return mean + std * multiplier;
    }
}
interface BPMListener {
    void onBpmDetected(int bpm);
    void onBpmInvalid();
}