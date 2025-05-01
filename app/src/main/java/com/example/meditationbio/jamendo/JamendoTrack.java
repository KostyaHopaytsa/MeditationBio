package com.example.meditationbio.jamendo;

public class JamendoTrack {
    public String name;
    public String artist;
    public String audioUrl;
    public String imageUrl;

    public JamendoTrack(String name, String artist, String audioUrl, String imageUrl) {
        this.name = name;
        this.artist = artist;
        this.audioUrl = audioUrl;
        this.imageUrl = imageUrl;
    }
}