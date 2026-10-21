package com.example.joemovies.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.joemovies.MainActivity;
import com.example.joemovies.R;
import com.example.joemovies.data.repository.AuthRepository;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword, etName, etUsername, etConfirmPassword, etAdminCode;
    private Button btnAction;
    private TextView tvSwitch, tvTitle;
    private ProgressBar progressBar;
    private View ivLogo;

    private boolean isLoginMode = true;
    private boolean isAdminUnlocked = false;
    private AuthRepository authRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        authRepository = new AuthRepository();

        ivLogo = findViewById(R.id.ivLogo);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etName = findViewById(R.id.etName);
        etUsername = findViewById(R.id.etUsername);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        etAdminCode = findViewById(R.id.etAdminCode);
        btnAction = findViewById(R.id.btnAction);
        tvSwitch = findViewById(R.id.tvSwitch);
        tvTitle = findViewById(R.id.tvTitle);
        progressBar = findViewById(R.id.progressBar);

        tvSwitch.setOnClickListener(v -> toggleMode());
        btnAction.setOnClickListener(v -> handleAction());

        // Gesto secreto: Mantener presionado el logo para revelar/ocultar el campo de Administrador
        ivLogo.setOnLongClickListener(v -> {
            isAdminUnlocked = !isAdminUnlocked;
            if (!isLoginMode) {
                etAdminCode.setVisibility(isAdminUnlocked ? View.VISIBLE : View.GONE);
            }
            Toast.makeText(this, isAdminUnlocked ? "🔓 Modo Administrador activado" : "🔒 Modo Administrador oculto", Toast.LENGTH_SHORT).show();
            return true;
        });
    }

    private void toggleMode() {
        isLoginMode = !isLoginMode;
        if (isLoginMode) {
            tvTitle.setText("Bienvenido de vuelta");
            etName.setVisibility(View.GONE);
            etEmail.setVisibility(View.GONE);
            etConfirmPassword.setVisibility(View.GONE);
            etAdminCode.setVisibility(View.GONE);
            btnAction.setText("Iniciar Sesión");
            tvSwitch.setText("¿No tienes cuenta? Regístrate");
        } else {
            tvTitle.setText("Crear una cuenta");
            etName.setVisibility(View.VISIBLE);
            etEmail.setVisibility(View.VISIBLE);
            etConfirmPassword.setVisibility(View.VISIBLE);
            etAdminCode.setVisibility(isAdminUnlocked ? View.VISIBLE : View.GONE);
            btnAction.setText("Registrarse");
            tvSwitch.setText("¿Ya tienes cuenta? Inicia sesión");
        }
    }

    private void handleAction() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isLoginMode) {
            performLogin(username, password);
        } else {
            performRegister(username, password);
        }
    }

    private void performLogin(String username, String password) {
        setLoading(true);
        authRepository.loginByUsername(username, password, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(String message) {
                navigateToMain();
            }

            @Override
            public void onError(String error) {
                showError(error);
            }
        });
    }

    private void performRegister(String username, String password) {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();
        String adminCode = etAdminCode.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);
        authRepository.register(username, email, password, name, adminCode, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(String message) {
                navigateToMain();
            }

            @Override
            public void onError(String error) {
                showError(error);
            }
        });
    }

    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnAction.setEnabled(!isLoading);
    }

    private void navigateToMain() {
        com.example.joemovies.data.SessionManager.updateLastActivity(this);
        com.google.firebase.auth.FirebaseUser currentUser = com.example.joemovies.data.FirebaseService.getAuth().getCurrentUser();
        if (currentUser != null) {
            authRepository.getUserRole(currentUser.getUid(), new AuthRepository.RoleCallback() {
                @Override
                public void onRoleFetched(String role) {
                    Intent intent;
                    if ("ADMIN".equals(role)) {
                        intent = new Intent(LoginActivity.this, com.example.joemovies.ui.admin.AdminActivity.class);
                    } else {
                        intent = new Intent(LoginActivity.this, MainActivity.class);
                    }
                    startActivity(intent);
                    finish();
                }

                @Override
                public void onError(String error) {
                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    finish();
                }
            });
        } else {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }

    private void showError(String error) {
        setLoading(false);
        Toast.makeText(this, "Error: " + error, Toast.LENGTH_LONG).show();
    }
}
