package com.example.meditationbio;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import com.example.meditationbio.data.db.AppDatabase;
import com.example.meditationbio.data.model.Measurement;
import java.util.List;

public class MeasurementViewModel extends AndroidViewModel {

    private final LiveData<List<Measurement>> measurements;

    public MeasurementViewModel(@NonNull Application application) {
        super(application);
        AppDatabase db = AppDatabase.getInstance(application);
        measurements = db.measurementDao().getAllMeasurements();
    }

    public LiveData<List<Measurement>> getMeasurements() {
        return measurements;
    }
}
