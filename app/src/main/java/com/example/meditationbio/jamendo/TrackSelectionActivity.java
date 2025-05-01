package com.example.meditationbio.jamendo;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.meditationbio.R;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

public class TrackSelectionActivity extends AppCompatActivity {

    private ExoPlayer player;
    private SeekBar seekBar;
    private Button playPauseButton;
    private Handler handler = new Handler();
    private boolean isPlaying = false;

    private final Runnable updateSeekRunnable = new Runnable() {
        @Override
        public void run() {
            if (player != null && player.isPlaying()) {
                seekBar.setProgress((int) player.getCurrentPosition());
                handler.postDelayed(this, 500);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track_selection);

        RecyclerView recyclerView = findViewById(R.id.trackRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        seekBar = findViewById(R.id.seekBar);
        playPauseButton = findViewById(R.id.playPauseButton);

        String tracksJson = getIntent().getStringExtra("tracks_json");
        if (tracksJson == null) {
            Toast.makeText(this, "Немає треків для відображення", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Type type = new TypeToken<List<JamendoTrack>>(){}.getType();
        List<JamendoTrack> tracks = new Gson().fromJson(tracksJson, type);

        TrackAdapter adapter = new TrackAdapter(tracks, this::playTrack);
        recyclerView.setAdapter(adapter);

        playPauseButton.setOnClickListener(v -> {
            if (player != null) {
                if (player.isPlaying()) {
                    player.pause();
                    playPauseButton.setText("▶");
                } else {
                    player.play();
                    playPauseButton.setText("⏸");
                }
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (player != null && fromUser) {
                    player.seekTo(progress);
                }
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void playTrack(JamendoTrack track) {
        if (player != null) {
            player.release();
        }

        player = new ExoPlayer.Builder(this).build();
        player.setMediaItem(MediaItem.fromUri(Uri.parse(track.audioUrl)));
        player.prepare();
        player.play();
        isPlaying = true;
        playPauseButton.setText("⏸");

        Toast.makeText(this, "Грає: " + track.name, Toast.LENGTH_SHORT).show();

        player.addListener(new androidx.media3.common.Player.Listener() {
            @Override
            public void onIsPlayingChanged(boolean isPlayingNow) {
                if (isPlayingNow) {
                    seekBar.setMax((int) player.getDuration());
                    handler.post(updateSeekRunnable);
                } else {
                    handler.removeCallbacks(updateSeekRunnable);
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) {
            handler.removeCallbacks(updateSeekRunnable);
            player.release();
        }
    }
}
