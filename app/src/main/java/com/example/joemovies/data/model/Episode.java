package com.example.joemovies.data.model;

import java.io.Serializable;

public class Episode implements Serializable {
    private int seasonNumber;
    private int episodeNumber;
    private String title;
    private String videoUrl;
    private String duration;
    private String imageUrl;

    public Episode() {
        // Requerido para Firebase
        this.seasonNumber = 1;
        this.episodeNumber = 1;
    }

    public Episode(int seasonNumber, int episodeNumber, String title, String videoUrl, String duration) {
        this(seasonNumber, episodeNumber, title, videoUrl, duration, "");
    }

    public Episode(int seasonNumber, int episodeNumber, String title, String videoUrl, String duration, String imageUrl) {
        this.seasonNumber = seasonNumber;
        this.episodeNumber = episodeNumber;
        this.title = title;
        this.videoUrl = videoUrl;
        this.duration = duration;
        this.imageUrl = imageUrl;
    }

    public int getSeasonNumber() { return seasonNumber; }
    public void setSeasonNumber(int seasonNumber) { this.seasonNumber = seasonNumber; }

    public int getEpisodeNumber() { return episodeNumber; }
    public void setEpisodeNumber(int episodeNumber) { this.episodeNumber = episodeNumber; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }

    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}
