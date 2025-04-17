package com.example.meditationbio;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private PreviewView previewView;
    private TextView bpmText, brpmText, stressText;
    private Button startButton, repeatBpmButton, nextToBrpmButton, repeatBrpmButton, nextToStressButton;

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private static final int REQUEST_AUDIO_PERMISSION = 102;

    private boolean bpmMeasured = false;
    private boolean brpmMeasured = false;

    private ProcessCameraProvider cameraProvider;
    private ImageAnalysis imageAnalysis;
    private Camera camera; // 👈 для керування спалахом

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        previewView = findViewById(R.id.previewView);
        bpmText = findViewById(R.id.bpmText);
        brpmText = findViewById(R.id.brpmText);
        stressText = findViewById(R.id.stressText);

        startButton = findViewById(R.id.startButton);
        repeatBpmButton = findViewById(R.id.repeatBpmButton);
        nextToBrpmButton = findViewById(R.id.nextToBrpmButton);
        repeatBrpmButton = findViewById(R.id.repeatBrpmButton);
        nextToStressButton = findViewById(R.id.nextToStressButton);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        startButton.setOnClickListener(v -> startBpmAnalysis());

        repeatBpmButton.setOnClickListener(v -> startBpmAnalysis());
        nextToBrpmButton.setOnClickListener(v -> {
            repeatBpmButton.setVisibility(View.GONE);
            nextToBrpmButton.setVisibility(View.GONE);
            startBrpmAnalysis();
        });

        repeatBrpmButton.setOnClickListener(v -> startBrpmAnalysis());
        nextToStressButton.setOnClickListener(v -> {
            repeatBrpmButton.setVisibility(View.GONE);
            nextToStressButton.setVisibility(View.GONE);
            startStressAnalyzer();
        });

        initCameraProvider(); // Просто ініціалізуємо, не запускаємо
    }

    private void initCameraProvider() {
        ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(this);
        future.addListener(() -> {
            try {
                cameraProvider = future.get();
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @OptIn(markerClass = ExperimentalGetImage.class)
    private void startBpmAnalysis() {
        bpmText.setText("BPM: ...");
        bpmMeasured = false;

        if (cameraProvider == null) {
            Toast.makeText(this, "Камеру не ініціалізовано", Toast.LENGTH_SHORT).show();
            return;
        }

        imageAnalysis = new ImageAnalysis.Builder()
                .setTargetResolution(new Size(640, 480))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor(), new PPGAnalyzer(bpm -> {
            runOnUiThread(() -> {
                bpmText.setText("BPM: " + bpm);
                bpmMeasured = true;
                stopCamera(); // Вимикаємо камеру і спалах після виміру
                repeatBpmButton.setVisibility(View.VISIBLE);
                nextToBrpmButton.setVisibility(View.VISIBLE);
            });
        }));

        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        CameraSelector selector = CameraSelector.DEFAULT_BACK_CAMERA;

        cameraProvider.unbindAll();
        camera = cameraProvider.bindToLifecycle(this, selector, preview, imageAnalysis);

        // 🔦 Увімкнути спалах
        if (camera != null) {
            camera.getCameraControl().enableTorch(true);
        }
    }

    private void stopCamera() {
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }

        // 🔦 Вимкнути спалах
        if (camera != null) {
            camera.getCameraControl().enableTorch(false);
            camera = null;
        }
    }

    private void startBrpmAnalysis() {
        brpmText.setText("BRPM: ...");
        brpmMeasured = false;

        BreathAnalyzer analyzer = new BreathAnalyzer(brpm -> {
            runOnUiThread(() -> {
                brpmText.setText("BRPM: " + brpm);
                repeatBrpmButton.setVisibility(View.VISIBLE);
                nextToStressButton.setVisibility(View.VISIBLE);
            });
        });

        if (accelerometer != null) {
            sensorManager.registerListener(analyzer, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        } else {
            Toast.makeText(this, "Акселерометр не знайдено", Toast.LENGTH_SHORT).show();
        }
    }

    private void startStressAnalyzer() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {

            stressText.setText("STRESS: ...");

            StressAnalyzer stressAnalyzer = new StressAnalyzer(level -> {
                runOnUiThread(() -> {
                    stressText.setText("STRESS: " + level);
                    Toast.makeText(this, "Stress level: " + level, Toast.LENGTH_LONG).show();
                });
            });

            stressAnalyzer.startRecording();

        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_AUDIO_PERMISSION);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopCamera();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_AUDIO_PERMISSION &&
                grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startStressAnalyzer();
        }
    }
}