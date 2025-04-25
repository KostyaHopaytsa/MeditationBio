package com.example.meditationbio.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.meditationbio.data.model.Measurement;

import java.util.List;

@Dao
public interface MeasurementDao {
    @Insert
    void insert(Measurement measurement);

    @Query("SELECT * FROM Measurement ORDER BY timestamp DESC")
    List<Measurement> getAllMeasurements();
}
