package com.example.meditationbio;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.meditationbio.dataDB.Measurement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MeasurementAdapter extends RecyclerView.Adapter<MeasurementAdapter.ViewHolder> {

    private final List<Measurement> measurements;

    public MeasurementAdapter(List<Measurement> measurements) {
        this.measurements = measurements;
    }

    @NonNull
    @Override
    public MeasurementAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_measurement, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MeasurementAdapter.ViewHolder holder, int position) {
        Measurement m = measurements.get(position);
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
        String date = sdf.format(new Date(m.timestamp));

        holder.dateTime.setText(date);
        holder.bpm.setText("BPM: " + m.bpm);
        holder.brpm.setText("BRPM: " + m.brpm);
        holder.stress.setText("Стрес: " + m.stress);
        holder.state.setText("Стан: " + m.overallState);
    }

    @Override
    public int getItemCount() {
        return measurements.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView dateTime, bpm, brpm, stress, state;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            dateTime = itemView.findViewById(R.id.dateTime);
            bpm = itemView.findViewById(R.id.bpm);
            brpm = itemView.findViewById(R.id.brpm);
            stress = itemView.findViewById(R.id.stress);
            state = itemView.findViewById(R.id.state);
        }
    }
}
