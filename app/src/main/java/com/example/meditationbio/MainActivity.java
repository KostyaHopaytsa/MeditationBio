package com.example.meditationbio;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.MotionEvent;
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

import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.common.MediaItem;
import com.google.common.util.concurrent.ListenableFuture;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import com.example.meditationbio.data.db.AppDatabase;
import com.example.meditationbio.data.model.Measurement;

public class MainActivity extends AppCompatActivity {

    private PreviewView previewView;
    private TextView bpmText, brpmText, stressText;
    private Button startButton, repeatBpmButton, nextToBrpmButton, repeatBrpmButton, nextToStressButton, recordButton;

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private static final int REQUEST_AUDIO_PERMISSION = 102;

    private boolean bpmMeasured = false;
    private boolean brpmMeasured = false;

    private ProcessCameraProvider cameraProvider;
    private ImageAnalysis imageAnalysis;
    private Camera camera;

    private StressAnalyzer stressAnalyzer;

    private Integer bpmValue = null;
    private Integer brpmValue = null;
    private String stressLevel = null;
    private AppDatabase db;

    private JamendoID JamendoID;

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
        recordButton = findViewById(R.id.recordButton);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        db = AppDatabase.getInstance(getApplicationContext());

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

        recordButton.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    stressText.setText("Запис голосу...");
                    startVoiceRecording();
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    stopVoiceRecording();
                    recordButton.setVisibility(View.GONE);
                    return true;
            }
            return false;
        });

        initCameraProvider();
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
                bpmValue = bpm;
                bpmMeasured = true;
                stopCamera();
                repeatBpmButton.setVisibility(View.VISIBLE);
                nextToBrpmButton.setVisibility(View.VISIBLE);
                checkAndProcessFinalState();
            });
        }));

        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        CameraSelector selector = CameraSelector.DEFAULT_BACK_CAMERA;
        cameraProvider.unbindAll();
        camera = cameraProvider.bindToLifecycle(this, selector, preview, imageAnalysis);

        if (camera != null) {
            camera.getCameraControl().enableTorch(true);
        }
    }

    private void stopCamera() {
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
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
                brpmValue = brpm;
                repeatBrpmButton.setVisibility(View.VISIBLE);
                nextToStressButton.setVisibility(View.VISIBLE);
                checkAndProcessFinalState();
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
            recordButton.setVisibility(View.VISIBLE);

            stressAnalyzer = new StressAnalyzer(level -> {
                runOnUiThread(() -> {
                    stressText.setText("STRESS: " + level);
                    stressLevel = level;
                    Toast.makeText(this, "Stress level: " + level, Toast.LENGTH_LONG).show();
                    checkAndProcessFinalState();
                });
            });

        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_AUDIO_PERMISSION);
        }
    }

    private void startVoiceRecording() {
        if (stressAnalyzer != null) {
            stressAnalyzer.beginRecording();
        }
    }

    private void stopVoiceRecording() {
        if (stressAnalyzer != null) {
            stressAnalyzer.endAndAnalyze();
        }
    }

    private void checkAndProcessFinalState() {
        if (bpmValue != null && brpmValue != null && stressLevel != null) {
            String overallState = determineOverallState(bpmValue, brpmValue, stressLevel);
            Toast.makeText(this, "Загальний стан: " + overallState, Toast.LENGTH_LONG).show();

            // Зберегти в базу
            Measurement measurement = new Measurement(bpmValue, brpmValue, stressLevel, System.currentTimeMillis());
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            Executors.newSingleThreadExecutor().execute(() -> db.measurementDao().insert(measurement));

            // Отримати трек
            String tag = mapStateToJamendoTag(overallState);
            fetchTrackFromJamendo(tag);
        }
    }

    private String determineOverallState(int bpm, int brpm, String stress) {
        int score = 0;
        if (bpm > 90) score += 2;
        else if (bpm > 75) score += 1;
        if (brpm > 20) score += 2;
        else if (brpm > 15) score += 1;
        if ("Medium".equals(stress)) score += 1;
        else if ("High".equals(stress)) score += 2;

        if (score <= 1) return "Low";
        if (score <= 3) return "Medium";
        return "High";
    }

    private String mapStateToJamendoTag(String state) {
        switch (state) {
            case "High":
                return "calm+meditation";
            case "Medium":
                return "downtempo";
            case "Low":
            default:
                return "ambient+chillout";
        }
    }

    private void fetchTrackFromJamendo(String moodTag) {
        String clientId = JamendoID.ID; // 🔐 замінити на реальний
        String url = "https://api.jamendo.com/v3.0/tracks/?" +
                "client_id=" + clientId +
                "&format=json" +
                "&limit=1" +
                "&tags=" + moodTag +
                "&include=musicinfo" +
                "&audioformat=mp32";

        new Thread(() -> {
            try {
                URL requestUrl = new URL(url);
                HttpURLConnection connection = (HttpURLConnection) requestUrl.openConnection();
                connection.setRequestMethod("GET");

                InputStream inputStream = connection.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder result = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }

                JSONObject response = new JSONObject(result.toString());
                JSONArray tracks = response.getJSONArray("results");

                if (tracks.length() > 0) {
                    JSONObject track = tracks.getJSONObject(0);
                    String streamUrl = track.getString("audio");
                    runOnUiThread(() -> playMusic(streamUrl));
                } else {
                    runOnUiThread(() -> Toast.makeText(this, "Треків не знайдено", Toast.LENGTH_SHORT).show());
                }

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Помилка підключення до Jamendo", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void playMusic(String url) {
        ExoPlayer player = new ExoPlayer.Builder(this).build();
        player.setMediaItem(MediaItem.fromUri(Uri.parse(url)));
        player.prepare();
        player.play();
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