package com.example.joemovies.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.joemovies.R;
import com.example.joemovies.data.FirebaseService;
import com.example.joemovies.data.model.Episode;
import com.example.joemovies.data.model.MovieContent;
import com.example.joemovies.data.repository.AuthRepository;
import com.example.joemovies.ui.adapter.EpisodeAdapter;
import com.example.joemovies.ui.admin.AddContentActivity;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class DetailActivity extends AppCompatActivity {

    private AuthRepository authRepository;
    private final boolean[] isUserAdmin = new boolean[]{false};
    private final boolean[] isProcessingContent = new boolean[]{false};
    private final List<Episode> episodeList = new ArrayList<>();
    private EpisodeAdapter episodeAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        authRepository = new AuthRepository();

        findViewById(R.id.btnBackDetail).setOnClickListener(v -> finish());

        // Recibir datos
        String movieId = getIntent().getStringExtra("id");
        String title = getIntent().getStringExtra("title");
        String type = getIntent().getStringExtra("type");
        String genre = getIntent().getStringExtra("genre");
        String description = getIntent().getStringExtra("description");
        String director = getIntent().getStringExtra("director");
        String duration = getIntent().getStringExtra("duration");
        int year = getIntent().getIntExtra("year", 2024);
        double rating = getIntent().getDoubleExtra("rating", 0.0);
        String imageUrl = getIntent().getStringExtra("imageUrl");
        String bannerUrl = getIntent().getStringExtra("bannerUrl");
        String videoUrl = getIntent().getStringExtra("videoUrl");

        // Vincular vistas
        TextView tvTitle = findViewById(R.id.tvDetailTitle);
        TextView tvType = findViewById(R.id.tvDetailType);
        TextView tvRating = findViewById(R.id.tvDetailRating);
        TextView tvYear = findViewById(R.id.tvDetailYear);
        TextView tvDuration = findViewById(R.id.tvDetailDuration);
        TextView tvDescription = findViewById(R.id.tvDetailDescription);
        TextView tvDirector = findViewById(R.id.tvDetailDirector);
        TextView tvProcessingBadge = findViewById(R.id.tvProcessingBadge);
        ImageView ivBackdrop = findViewById(R.id.ivDetailBackdrop);
        Button btnPlay = findViewById(R.id.btnPlay);
        Button btnViewCinemas = findViewById(R.id.btnViewCinemas);
        LinearLayout llCinemaBanner = findViewById(R.id.llCinemaBanner);

        LinearLayout llAdminActions = findViewById(R.id.llAdminActions);
        Button btnEditContent = findViewById(R.id.btnEditContent);
        Button btnDeleteContent = findViewById(R.id.btnDeleteContent);

        LinearLayout llEpisodesContainer = findViewById(R.id.llEpisodesContainer);
        RecyclerView rvDetailEpisodes = findViewById(R.id.rvDetailEpisodes);
        TextView tvNoEpisodes = findViewById(R.id.tvNoEpisodes);

        LinearLayout llRatingContainer = findViewById(R.id.llRatingContainer);
        android.widget.RatingBar ratingBarUser = findViewById(R.id.ratingBarUser);
        TextView tvRatingPrompt = findViewById(R.id.tvRatingPrompt);

        episodeAdapter = new EpisodeAdapter(episodeList);
        rvDetailEpisodes.setAdapter(episodeAdapter);

        boolean isSerie = "Serie".equalsIgnoreCase(type);
        if (isSerie) {
            llEpisodesContainer.setVisibility(View.VISIBLE);
            btnPlay.setText("▶️ Selecciona un Capítulo de la Lista");
        }

        // Mostrar datos iniciales
        tvTitle.setText(title);
        tvType.setText(type != null ? type.toUpperCase() : "CONTENIDO");
        tvRating.setText("⭐ " + rating);
        tvYear.setText(String.valueOf(year));
        tvDuration.setText(duration);
        tvDescription.setText(description);
        tvDirector.setText("Director: " + director);

        // Cargar imagen de fondo / banner
        String displayImage = (bannerUrl != null && !bannerUrl.isEmpty()) ? bannerUrl : imageUrl;
        if (displayImage != null && !displayImage.isEmpty()) {
            Glide.with(this)
                    .load(com.example.joemovies.utils.ImageUtils.getDirectImageUrl(displayImage))
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_gallery)
                    .into(ivBackdrop);
        }

        // Verificar si el usuario es ADMIN y manejar Votos de Usuarios
        FirebaseUser user = FirebaseService.getAuth().getCurrentUser();
        if (user != null) {
            authRepository.getUserRole(user.getUid(), new AuthRepository.RoleCallback() {
                @Override
                public void onRoleFetched(String role) {
                    if ("ADMIN".equals(role)) {
                        isUserAdmin[0] = true;
                        llAdminActions.setVisibility(View.VISIBLE);
                        if (llRatingContainer != null) {
                            llRatingContainer.setVisibility(View.GONE);
                        }
                        if (isSerie && episodeList.isEmpty()) {
                            btnPlay.setText("＋ Editar Serie para Agregar Capítulos");
                        }
                    }
                }

                @Override
                public void onError(String error) {}
            });
        }

        // Votación y Calificación de Usuarios
        if (movieId != null) {
            FirebaseService.getDatabaseRef().child("catalog").child(movieId).child("userRatings")
                    .addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                double sum = 0;
                                long count = snapshot.getChildrenCount();
                                for (DataSnapshot voteSnap : snapshot.getChildren()) {
                                    Double vote = voteSnap.getValue(Double.class);
                                    if (vote != null) {
                                        sum += vote;
                                    }
                                }
                                if (count > 0) {
                                    double avg = Math.round(((sum / count) * 2) * 10.0) / 10.0;
                                    tvRating.setText("⭐ " + avg + " (" + count + " votos)");
                                    FirebaseService.getDatabaseRef().child("catalog").child(movieId).child("rating").setValue(avg);
                                }
                            }

                            if (user != null && snapshot.hasChild(user.getUid())) {
                                Double myVote = snapshot.child(user.getUid()).getValue(Double.class);
                                if (myVote != null) {
                                    ratingBarUser.setRating(myVote.floatValue());
                                    tvRatingPrompt.setText("Tu voto: " + myVote.intValue() + " ⭐");
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {}
                    });

            ratingBarUser.setOnRatingBarChangeListener((ratingBar, ratingVal, fromUser) -> {
                if (fromUser && user != null) {
                    FirebaseService.getDatabaseRef().child("catalog").child(movieId)
                            .child("userRatings").child(user.getUid()).setValue(ratingVal)
                            .addOnSuccessListener(aVoid -> Toast.makeText(DetailActivity.this, "¡Gracias por calificar!", Toast.LENGTH_SHORT).show());
                }
            });
        }

        // Escuchar datos en tiempo real (Episodios y Estado de Procesamiento)
        if (movieId != null) {
            FirebaseService.getDatabaseRef().child("catalog").child(movieId)
                    .addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            MovieContent content = snapshot.getValue(MovieContent.class);
                            if (content != null) {
                                isProcessingContent[0] = content.isProcessing();
                                tvProcessingBadge.setVisibility(content.isProcessing() ? View.VISIBLE : View.GONE);

                                if (llCinemaBanner != null) {
                                    llCinemaBanner.setVisibility(content.isAlliedCinemasEnabled() ? View.VISIBLE : View.GONE);
                                }

                                boolean contentIsSerie = "Serie".equalsIgnoreCase(content.getType());
                                if (contentIsSerie) {
                                    llEpisodesContainer.setVisibility(View.VISIBLE);
                                    if (content.getEpisodes() != null && !content.getEpisodes().isEmpty()) {
                                        tvNoEpisodes.setVisibility(View.GONE);
                                        rvDetailEpisodes.setVisibility(View.VISIBLE);
                                        episodeList.clear();
                                        episodeList.addAll(content.getEpisodes());
                                        episodeAdapter.notifyDataSetChanged();
                                        btnPlay.setText("▶️ Reproducir Capítulo 1");
                                    } else {
                                        tvNoEpisodes.setVisibility(View.VISIBLE);
                                        rvDetailEpisodes.setVisibility(View.GONE);
                                        if (isUserAdmin[0]) {
                                            btnPlay.setText("＋ Editar Serie para Agregar Capítulos");
                                        } else {
                                            btnPlay.setText("Proximamente más capítulos");
                                        }
                                    }
                                } else {
                                    llEpisodesContainer.setVisibility(View.GONE);
                                    btnPlay.setText("Reproducir");
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {}
                    });
        }

        btnPlay.setOnClickListener(v -> {
            if (isProcessingContent[0] && !isUserAdmin[0]) {
                Toast.makeText(this, "⏳ Este contenido se está procesando. Estará disponible muy pronto.", Toast.LENGTH_LONG).show();
                return;
            }

            if (videoUrl != null && !videoUrl.trim().isEmpty()) {
                Intent intent = new Intent(this, com.example.joemovies.ui.player.PlayerActivity.class);
                intent.putExtra("title", title);
                intent.putExtra("videoUrl", videoUrl);
                startActivity(intent);
            } else if (!episodeList.isEmpty()) {
                // Reproducir el primer episodio de la serie
                Episode firstEp = episodeList.get(0);
                Intent intent = new Intent(this, com.example.joemovies.ui.player.PlayerActivity.class);
                intent.putExtra("title", firstEp.getTitle());
                intent.putExtra("videoUrl", firstEp.getVideoUrl());
                startActivity(intent);
            } else if (isUserAdmin[0]) {
                // Si es admin y no hay capitulos, abrir la pantalla de edición para agregarlos
                Intent intent = new Intent(this, AddContentActivity.class);
                intent.putExtra("id", movieId);
                intent.putExtra("title", title);
                intent.putExtra("type", type);
                intent.putExtra("genre", genre);
                intent.putExtra("description", description);
                intent.putExtra("director", director);
                intent.putExtra("duration", duration);
                intent.putExtra("year", year);
                intent.putExtra("rating", rating);
                intent.putExtra("imageUrl", imageUrl);
                intent.putExtra("bannerUrl", bannerUrl);
                intent.putExtra("videoUrl", videoUrl);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(this, "Aún no hay capítulos cargados para esta serie.", Toast.LENGTH_SHORT).show();
            }
        });

        btnViewCinemas.setOnClickListener(v -> {
            Intent intent = new Intent(this, CineMapActivity.class);
            intent.putExtra(CineMapActivity.EXTRA_MOVIE_ID, movieId);
            intent.putExtra(CineMapActivity.EXTRA_MOVIE_TITLE, title);
            startActivity(intent);
        });

        // Configurar acciones de administrador (Editar / Eliminar)
        btnEditContent.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddContentActivity.class);
            intent.putExtra("id", movieId);
            intent.putExtra("title", title);
            intent.putExtra("type", type);
            intent.putExtra("genre", genre);
            intent.putExtra("description", description);
            intent.putExtra("director", director);
            intent.putExtra("duration", duration);
            intent.putExtra("year", year);
            intent.putExtra("rating", rating);
            intent.putExtra("imageUrl", imageUrl);
            intent.putExtra("bannerUrl", bannerUrl);
            intent.putExtra("videoUrl", videoUrl);
            startActivity(intent);
            finish();
        });

        btnDeleteContent.setOnClickListener(v -> {
            if (movieId == null) return;
            new AlertDialog.Builder(this)
                    .setTitle("Eliminar contenido")
                    .setMessage("¿Estás seguro de que deseas eliminar '" + title + "' del catálogo?")
                    .setPositiveButton("Eliminar", (dialog, which) -> {
                        FirebaseService.getDatabaseRef().child("catalog").child(movieId).removeValue()
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(DetailActivity.this, "Contenido eliminado exitosamente", Toast.LENGTH_SHORT).show();
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(DetailActivity.this, "Error al eliminar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        });
    }
}
