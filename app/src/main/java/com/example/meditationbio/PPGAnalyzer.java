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
    private static final int SMOOTHING_WINDOW = 8;
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
        double[] smooth = new double[raw.length];

        // Просте ковзне середнє
        for (int i = 0; i < raw.length; i++) {
            int start = Math.max(0, i - SMOOTHING_WINDOW);
            int end = Math.min(raw.length - 1, i + SMOOTHING_WINDOW);
            double sum = 0;
            for (int j = start; j <= end; j++) {
                sum += raw[j];
            }
            smooth[i] = sum / (end - start + 1);
        }

        // Пошук піків
        int peaks = 0;
        for (int i = 1; i < smooth.length - 1; i++) {
            if (smooth[i] > smooth[i - 1] && smooth[i] > smooth[i + 1]) {
                peaks++;
            }
        }

        // Розрахунок BPM
        double bpm = (double) peaks * 60 / (MAX_BUFFER_SIZE / 30.0);

        if (bpm < BPM_MIN || bpm > BPM_MAX) {
            Log.w("PPG", "BPM out of range: " + bpm);
            return;
        }

        int roundedBpm = (int) bpm;
        Log.d("PPG", "Final BPM: " + roundedBpm);

        if (bpmListener != null) {
            bpmListener.onBpmDetected(roundedBpm);
        }
    }
}

interface BPMListener {
    void onBpmDetected(int bpm);
}