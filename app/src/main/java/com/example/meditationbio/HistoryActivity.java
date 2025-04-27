package com.example.meditationbio;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class HistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private MeasurementAdapter adapter;
    private MeasurementViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        recyclerView = findViewById(R.id.measurementRecycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        viewModel = new ViewModelProvider(this).get(MeasurementViewModel.class);

        viewModel.getMeasurements().observe(this, measurements -> {
            adapter = new MeasurementAdapter(measurements);
            recyclerView.setAdapter(adapter);
        });
    }
}