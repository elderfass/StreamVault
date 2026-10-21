package com.example.joemovies.ui.splash;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.VideoView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.joemovies.MainActivity;
import com.example.joemovies.R;
import com.example.joemovies.data.FirebaseService;
import com.example.joemovies.data.SessionManager;
import com.example.joemovies.data.repository.AuthRepository;
import com.example.joemovies.ui.admin.AdminActivity;
import com.example.joemovies.ui.auth.LoginActivity;
import com.google.firebase.auth.FirebaseUser;

public class SplashActivity extends AppCompatActivity {

    private boolean isNavigated = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        VideoView videoView = findViewById(R.id.videoViewSplash);
        String videoPath = "android.resource://" + getPackageName() + "/" + R.raw.splash_video;
        videoView.setVideoURI(Uri.parse(videoPath));

        videoView.setOnCompletionListener(mp -> navigateNext());
        videoView.setOnErrorListener((mp, what, extra) -> {
            navigateNext();
            return true;
        });

        videoView.start();

        // Fallback por seguridad si el video demora o falla
        videoView.postDelayed(() -> {
            if (!isNavigated) {
                navigateNext();
            }
        }, 7000); // 7 segundos máximo
    }

    private synchronized void navigateNext() {
        if (isNavigated) return;
        isNavigated = true;

        VideoView videoView = findViewById(R.id.videoViewSplash);
        if (videoView != null) {
            videoView.animate().alpha(0f).setDuration(500).start();
        }

        videoView.postDelayed(() -> {
            FirebaseUser currentUser = FirebaseService.getAuth().getCurrentUser();
            if (currentUser != null && !SessionManager.isSessionExpired(this)) {
                SessionManager.updateLastActivity(this);
                new AuthRepository().getUserRole(currentUser.getUid(), new AuthRepository.RoleCallback() {
                    @Override
                    public void onRoleFetched(String role) {
                        Intent intent;
                        if ("ADMIN".equals(role)) {
                            intent = new Intent(SplashActivity.this, AdminActivity.class);
                        } else {
                            intent = new Intent(SplashActivity.this, MainActivity.class);
                        }
                        startActivity(intent);
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                        finish();
                    }

                    @Override
                    public void onError(String error) {
                        startActivity(new Intent(SplashActivity.this, MainActivity.class));
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                        finish();
                    }
                });
            } else {
                if (currentUser != null) {
                    FirebaseService.getAuth().signOut();
                }
                SessionManager.clearSession(this);
                startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                finish();
            }
        }, 400);
    }
}
