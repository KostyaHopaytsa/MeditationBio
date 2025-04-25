package com.example.meditationbio.data.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class Measurement {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public int bpm;
    public int brpm;
    public String stress;
    public long timestamp;

    public Measurement(int bpm, int brpm, String stress, long timestamp) {
        this.bpm = bpm;
        this.brpm = brpm;
        this.stress = stress;
        this.timestamp = timestamp;
    }
}