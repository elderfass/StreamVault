package com.example.joemovies.data.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MovieContent {
    private String id;
    private String title;
    private String type; // "Pelicula" o "Serie"
    private String genre;
    private int year;
    private double rating;
    private String duration;
    private String description;
    private String director;
    private String imageUrl;
    private String bannerUrl;
    private String videoUrl;
    private boolean processing; // true si está procesándose
    private boolean monthlyPremiere; // true si es Estreno del Mes
    private boolean alliedCinemasEnabled; // true si tiene cines aliados habilitados por Admin
    private long premiereExpirationTimestamp; // fecha limite de la publicidad del estreno
    private List<Episode> episodes;
    private List<Cinema> alliedCinemas;

    public MovieContent() {
        this.episodes = new ArrayList<>();
        this.alliedCinemas = new ArrayList<>();
        this.processing = false;
        this.monthlyPremiere = false;
        this.alliedCinemasEnabled = false;
    }

    public MovieContent(String title, String type, String genre, int year, double rating, String duration, String description, String director) {
        this(title, type, genre, year, rating, duration, description, director, "", "", "");
    }

    public MovieContent(String title, String type, String genre, int year, double rating, String duration, String description, String director, String imageUrl, String bannerUrl, String videoUrl) {
        this.id = UUID.randomUUID().toString();
        this.title = title;
        this.type = type;
        this.genre = genre;
        this.year = year;
        this.rating = rating;
        this.duration = duration;
        this.description = description;
        this.director = director;
        this.imageUrl = imageUrl;
        this.bannerUrl = bannerUrl;
        this.videoUrl = videoUrl;
        this.processing = false;
        this.episodes = new ArrayList<>();
    }

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }
    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }
    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }
    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getDirector() { return director; }
    public void setDirector(String director) { this.director = director; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getBannerUrl() { return bannerUrl; }
    public void setBannerUrl(String bannerUrl) { this.bannerUrl = bannerUrl; }

    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }

    public boolean isProcessing() { return processing; }
    public void setProcessing(boolean processing) { this.processing = processing; }

    public boolean isMonthlyPremiere() { return monthlyPremiere; }
    public void setMonthlyPremiere(boolean monthlyPremiere) { this.monthlyPremiere = monthlyPremiere; }

    public long getPremiereExpirationTimestamp() { return premiereExpirationTimestamp; }
    public void setPremiereExpirationTimestamp(long premiereExpirationTimestamp) { this.premiereExpirationTimestamp = premiereExpirationTimestamp; }

    public boolean isPremiereActive() {
        if (!monthlyPremiere) return false;
        if (premiereExpirationTimestamp <= 0) return true; // sin vencimiento
        return System.currentTimeMillis() <= premiereExpirationTimestamp;
    }

    public boolean isAlliedCinemasEnabled() { return alliedCinemasEnabled; }
    public void setAlliedCinemasEnabled(boolean alliedCinemasEnabled) { this.alliedCinemasEnabled = alliedCinemasEnabled; }

    public List<Episode> getEpisodes() {
        if (episodes == null) {
            episodes = new ArrayList<>();
        }
        return episodes;
    }
    public void setEpisodes(List<Episode> episodes) { this.episodes = episodes; }

    public List<Cinema> getAlliedCinemas() {
        if (alliedCinemas == null) {
            alliedCinemas = new ArrayList<>();
        }
        return alliedCinemas;
    }
    public void setAlliedCinemas(List<Cinema> alliedCinemas) { this.alliedCinemas = alliedCinemas; }
}
