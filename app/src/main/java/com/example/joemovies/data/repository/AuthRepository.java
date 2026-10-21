package com.example.joemovies.data.repository;

import com.example.joemovies.data.FirebaseService;
import com.example.joemovies.data.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;

import java.util.HashMap;
import java.util.Map;

public class AuthRepository {
    private final FirebaseAuth auth;
    private final DatabaseReference db;

    public AuthRepository() {
        this.auth = FirebaseService.getAuth();
        this.db = FirebaseService.getDatabaseRef();
    }

    public interface AuthCallback {
        void onSuccess(String message);
        void onError(String error);
    }

    public interface RoleCallback {
        void onRoleFetched(String role);
        void onError(String error);
    }

    public void loginByUsername(String username, String password, AuthCallback callback) {
        db.child("usernames").child(username).get()
                .addOnSuccessListener(dataSnapshot -> {
                    if (dataSnapshot.exists()) {
                        String email = dataSnapshot.child("email").getValue(String.class);
                        if (email != null) {
                            auth.signInWithEmailAndPassword(email, password)
                                    .addOnSuccessListener(authResult -> callback.onSuccess("Login exitoso"))
                                    .addOnFailureListener(e -> callback.onError("Contraseña incorrecta"));
                        } else {
                            callback.onError("Error al recuperar el correo asociado");
                        }
                    } else {
                        callback.onError("El nombre de usuario no existe");
                    }
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    private static final String ADMIN_SECRET_KEY = "ADMIN123";

    public void register(String username, String email, String password, String name, AuthCallback callback) {
        register(username, email, password, name, "", callback);
    }

    public void register(String username, String email, String password, String name, String adminCode, AuthCallback callback) {
        // 1. Verificar si el nombre de usuario ya está tomado
        db.child("usernames").child(username).get()
                .addOnSuccessListener(dataSnapshot -> {
                    if (dataSnapshot.exists()) {
                        callback.onError("El nombre de usuario ya está en uso");
                    } else {
                        // 2. Crear el usuario en Auth
                        performRegistration(username, email, password, name, adminCode, callback);
                    }
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    private void performRegistration(String username, String email, String password, String name, String adminCode, AuthCallback callback) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    if (authResult.getUser() != null) {
                        String uid = authResult.getUser().getUid();
                        // Asignar rol según el código introducido
                        String role = (adminCode != null && ADMIN_SECRET_KEY.equalsIgnoreCase(adminCode.trim())) ? "ADMIN" : "USER";
                        User newUser = new User(uid, name, username, email, role);
                        
                        // Guardar en ambas ramas
                        saveUserToDatabase(newUser, callback);
                    } else {
                        callback.onError("Error al obtener datos del usuario creado");
                    }
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    private void saveUserToDatabase(User user, AuthCallback callback) {
        // Guardar perfil de usuario
        db.child("users").child(user.getUid()).setValue(user)
                .addOnSuccessListener(aVoid -> {
                    // Guardar mapeo de nombre de usuario
                    Map<String, String> usernameMap = new HashMap<>();
                    usernameMap.put("email", user.getEmail());
                    usernameMap.put("uid", user.getUid());
                    
                    db.child("usernames").child(user.getUsername()).setValue(usernameMap)
                            .addOnSuccessListener(aVoid2 -> callback.onSuccess("Registro exitoso"))
                            .addOnFailureListener(e -> callback.onError("Error al mapear usuario: " + e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onError("Error al guardar perfil: " + e.getMessage()));
    }

    public void getUserRole(String uid, RoleCallback callback) {
        db.child("users").child(uid).get()
                .addOnSuccessListener(dataSnapshot -> {
                    if (dataSnapshot.exists()) {
                        String role = dataSnapshot.child("role").getValue(String.class);
                        callback.onRoleFetched(role);
                    } else {
                        callback.onError("Usuario no encontrado en la base de datos");
                    }
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }
}
