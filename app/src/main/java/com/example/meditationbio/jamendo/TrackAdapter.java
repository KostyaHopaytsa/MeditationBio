package com.example.meditationbio.jamendo;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.meditationbio.R;
import com.squareup.picasso.Picasso;

import java.util.List;

public class TrackAdapter extends RecyclerView.Adapter<TrackAdapter.TrackViewHolder> {

    private final List<JamendoTrack> tracks;
    private final OnTrackClickListener listener;

    public interface OnTrackClickListener {
        void onTrackClick(JamendoTrack track);
    }

    public TrackAdapter(List<JamendoTrack> tracks, OnTrackClickListener listener) {
        this.tracks = tracks;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TrackViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_track, parent, false);
        return new TrackViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TrackViewHolder holder, int position) {
        JamendoTrack track = tracks.get(position);
        holder.bind(track);
    }

    @Override
    public int getItemCount() {
        return tracks.size();
    }

    class TrackViewHolder extends RecyclerView.ViewHolder {
        TextView trackName;
        TextView trackArtist;
        ImageView trackImage;

        TrackViewHolder(@NonNull View itemView) {
            super(itemView);
            trackName = itemView.findViewById(R.id.trackName);
            trackArtist = itemView.findViewById(R.id.trackArtist);
            trackImage = itemView.findViewById(R.id.trackImage);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    listener.onTrackClick(tracks.get(position));
                }
            });
        }

        void bind(JamendoTrack track) {
            trackName.setText(track.name);
            trackArtist.setText(track.artist);
            if (track.imageUrl != null && !track.imageUrl.isEmpty()) {
                Picasso.get()
                        .load(track.imageUrl)
                        .placeholder(R.drawable.ic_music_placeholder)
                        .into(trackImage);
            } else {
                trackImage.setImageResource(R.drawable.ic_music_placeholder);
            }
        }
    }
}