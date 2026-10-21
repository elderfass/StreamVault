package com.example.joemovies.data.model;

public class User {
    private String uid;
    private String name;
    private String username;
    private String email;
    private String role; // Siempre "USER" desde el cliente

    public User() {
        // Requerido para Firebase Realtime Database
    }

    public User(String uid, String name, String username, String email, String role) {
        this.uid = uid;
        this.name = name;
        this.username = username;
        this.email = email;
        this.role = role;
    }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
