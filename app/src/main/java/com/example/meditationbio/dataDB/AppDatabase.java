package com.example.meditationbio.dataDB;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {Measurement.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase instance;

    public abstract MeasurementDao measurementDao();

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "meditation_db")
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }
}