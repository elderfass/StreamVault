package com.example.joemovies.data.model;

import java.util.UUID;

public class Cinema {
    private String id;
    private String name;
    private String movieId;
    private String movieTitle;
    private String showtimes;
    private double latitude;
    private double longitude;
    private String address;

    public Cinema() {
        this.id = UUID.randomUUID().toString();
    }

    public Cinema(String name, String movieId, String movieTitle, String showtimes, double latitude, double longitude, String address) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.movieId = movieId;
        this.movieTitle = movieTitle;
        this.showtimes = showtimes;
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
    }

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getMovieId() { return movieId; }
    public void setMovieId(String movieId) { this.movieId = movieId; }

    public String getMovieTitle() { return movieTitle; }
    public void setMovieTitle(String movieTitle) { this.movieTitle = movieTitle; }

    public String getShowtimes() { return showtimes; }
    public void setShowtimes(String showtimes) { this.showtimes = showtimes; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
}
