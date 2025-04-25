package com.example.meditationbio;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.util.Log;

public class StressAnalyzer {

    private static final int SAMPLE_RATE = 16000;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    private final StressListener listener;
    private final Handler handler = new Handler();

    private AudioRecord recorder;
    private short[] buffer;
    private boolean isRecording = false;
    private int bufferSize;
    private int currentIndex = 0;

    public StressAnalyzer(StressListener listener) {
        this.listener = listener;
        this.bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
        this.buffer = new short[bufferSize * 5]; // до 5 сек
    }

    public void beginRecording() {
        try {
            recorder = new AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    bufferSize
            );

            if (recorder.getState() != AudioRecord.STATE_INITIALIZED) {
                Log.e("STRESS", "AudioRecord not initialized.");
                return;
            }

            isRecording = true;
            currentIndex = 0;
            recorder.startRecording();
            Log.d("STRESS", "Recording started (hold button)");

            new Thread(() -> {
                while (isRecording && currentIndex < buffer.length) {
                    int read = recorder.read(buffer, currentIndex, buffer.length - currentIndex);
                    if (read > 0) {
                        currentIndex += read;
                    }
                }
            }).start();

        } catch (SecurityException e) {
            Log.e("STRESS", "Permission error: " + e.getMessage());
            if (recorder != null) recorder.release();
        }
    }

    public void endAndAnalyze() {
        if (!isRecording || recorder == null) return;

        isRecording = false;
        try {
            recorder.stop();
        } catch (IllegalStateException e) {
            Log.e("STRESS", "Recorder stop failed: " + e.getMessage());
        }
        recorder.release();
        recorder = null;

        Log.d("STRESS", "Recording finished. Samples: " + currentIndex);
        analyze(buffer, currentIndex);
    }

    private void analyze(short[] buffer, int length) {
        if (length <= 0) return;

        double sumSq = 0;
        double maxAmp = 0;
        double[] norm = new double[length];

        for (int i = 0; i < length; i++) {
            norm[i] = buffer[i] / 32768.0;
            sumSq += norm[i] * norm[i];
            if (Math.abs(norm[i]) > maxAmp) maxAmp = Math.abs(norm[i]);
        }

        double rms = Math.sqrt(sumSq / length);

        double mean = sumSq / length;
        double std = 0;
        for (double val : norm) std += Math.pow(val * val - mean, 2);
        std = Math.sqrt(std / length);

        Log.d("STRESS", "RMS: " + rms + ", MaxAmp: " + maxAmp + ", StdDev: " + std);

        String level;
        if (rms < 0.02 && std < 0.01) {
            level = "Low";
        } else if (rms < 0.06 && std < 0.03) {
            level = "Medium";
        } else {
            level = "High";
        }

        if (listener != null) {
            handler.post(() -> listener.onStressLevelDetected(level));
        }
    }
}
interface StressListener {
    void onStressLevelDetected(String level); // "Low", "Medium", "High"
}