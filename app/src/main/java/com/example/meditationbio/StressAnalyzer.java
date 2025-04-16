package com.example.meditationbio;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.util.Log;

public class StressAnalyzer {

    private static final int SAMPLE_RATE = 16000; // 16 kHz — для голосу
    private static final int RECORD_DURATION_MS = 5000; // 5 сек
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    private final StressListener listener;
    private final Handler handler = new Handler();

    public StressAnalyzer(StressListener listener) {
        this.listener = listener;
    }

    public void startRecording() {
        int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);

        AudioRecord recorder = null;

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

            short[] audioBuffer = new short[bufferSize * RECORD_DURATION_MS / 1000];

            recorder.startRecording();
            Log.d("STRESS", "Recording started");

            final AudioRecord finalRecorder = recorder;
            new Thread(() -> {
                try {
                    int read = finalRecorder.read(audioBuffer, 0, audioBuffer.length);
                    finalRecorder.stop();
                    finalRecorder.release();

                    Log.d("STRESS", "Recording finished: " + read + " samples");

                    analyze(audioBuffer, read);
                } catch (SecurityException e) {
                    Log.e("STRESS", "Permission denied while reading audio: " + e.getMessage());
                }
            }).start();

        } catch (SecurityException e) {
            Log.e("STRESS", "AudioRecord initialization failed: " + e.getMessage());
            if (recorder != null) {
                recorder.release();
            }
        }
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

        // Стандартне відхилення амплітуди
        double mean = sumSq / length;
        double std = 0;
        for (double val : norm) std += Math.pow(val * val - mean, 2);
        std = Math.sqrt(std / length);

        Log.d("STRESS", "RMS: " + rms + ", MaxAmp: " + maxAmp + ", StdDev: " + std);

        // Оцінка рівня стресу
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