package com.example.meditationbio.dataDB;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.lifecycle.LiveData;

import java.util.List;

@Dao
public interface MeasurementDao {
    @Insert
    void insert(Measurement measurement);

    @Query("SELECT * FROM Measurement ORDER BY timestamp DESC")
    LiveData<List<Measurement>> getAllMeasurements();
}
