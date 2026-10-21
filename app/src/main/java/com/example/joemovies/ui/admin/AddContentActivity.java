package com.example.joemovies.ui.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.joemovies.R;
import com.example.joemovies.data.FirebaseService;
import com.example.joemovies.data.model.Cinema;
import com.example.joemovies.data.model.Episode;
import com.example.joemovies.data.model.MovieContent;
import com.example.joemovies.ui.adapter.EpisodeAdapter;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class AddContentActivity extends AppCompatActivity {

    private EditText etTitle, etYear, etDuration, etDescription, etDirector, etImageUrl, etVideoUrl;
    private CheckBox cbProcessing;
    private Spinner spType, spGenre;
    private Button btnSave, btnAddEpisode;
    private ImageView ivPosterPreview;
    private LinearLayout llSeriesSection;
    private EditText etEpSeason, etEpNumber, etEpTitle, etEpVideoUrl, etEpDuration, etEpImageUrl;
    private RecyclerView rvAddedEpisodes;
    private EpisodeAdapter episodeAdapter;
    private List<Episode> addedEpisodes;
    private int editingEpisodeIndex = -1;

    private boolean isEditMode = false;
    private String existingId = null;
    private double existingRating = 0.0;
    private boolean existingMonthlyPremiere = false;
    private boolean existingAlliedCinemasEnabled = false;
    private long existingExpirationTimestamp = 0;
    private List<Cinema> existingAlliedCinemas = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_content);

        etTitle = findViewById(R.id.etContentTitle);
        etYear = findViewById(R.id.etYear);
        etDuration = findViewById(R.id.etDuration);
        etDescription = findViewById(R.id.etDescription);
        etDirector = findViewById(R.id.etDirector);
        etImageUrl = findViewById(R.id.etImageUrl);
        etVideoUrl = findViewById(R.id.etVideoUrl);
        cbProcessing = findViewById(R.id.cbProcessing);
        spType = findViewById(R.id.spType);
        spGenre = findViewById(R.id.spGenre);
        btnSave = findViewById(R.id.btnSaveContent);
        ivPosterPreview = findViewById(R.id.ivPosterPreview);

        // Seccion de Series
        llSeriesSection = findViewById(R.id.llSeriesSection);
        etEpSeason = findViewById(R.id.etEpSeason);
        etEpNumber = findViewById(R.id.etEpNumber);
        etEpTitle = findViewById(R.id.etEpTitle);
        etEpVideoUrl = findViewById(R.id.etEpVideoUrl);
        etEpDuration = findViewById(R.id.etEpDuration);
        etEpImageUrl = findViewById(R.id.etEpImageUrl);
        btnAddEpisode = findViewById(R.id.btnAddEpisode);
        rvAddedEpisodes = findViewById(R.id.rvAddedEpisodes);

        addedEpisodes = new ArrayList<>();
        episodeAdapter = new EpisodeAdapter(addedEpisodes, true,
                position -> {
                    addedEpisodes.remove(position);
                    episodeAdapter.notifyDataSetChanged();
                    if (editingEpisodeIndex == position) {
                        resetEpisodeForm();
                    }
                },
                (position, episode) -> {
                    editingEpisodeIndex = position;
                    etEpSeason.setText(String.valueOf(episode.getSeasonNumber()));
                    etEpNumber.setText(String.valueOf(episode.getEpisodeNumber()));
                    etEpTitle.setText(episode.getTitle());
                    etEpVideoUrl.setText(episode.getVideoUrl());
                    etEpDuration.setText(episode.getDuration() != null ? episode.getDuration() : "");
                    etEpImageUrl.setText(episode.getImageUrl() != null ? episode.getImageUrl() : "");
                    btnAddEpisode.setText("✏️ Actualizar Capítulo");
                    Toast.makeText(this, "Editando: " + episode.getTitle(), Toast.LENGTH_SHORT).show();
                }
        );
        rvAddedEpisodes.setAdapter(episodeAdapter);

        // Configurar Spinners
        String[] types = {"Pelicula", "Serie", "Trailer"};
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, types);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spType.setAdapter(typeAdapter);

        String[] genres = {"Acción", "Sci-Fi", "Drama", "Thriller", "Comedia"};
        ArrayAdapter<String> genreAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, genres);
        genreAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spGenre.setAdapter(genreAdapter);

        spType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedType = types[position];
                boolean isSerie = "Serie".equalsIgnoreCase(selectedType);
                boolean isTrailer = "Trailer".equalsIgnoreCase(selectedType);

                llSeriesSection.setVisibility(isSerie ? View.VISIBLE : View.GONE);
                etVideoUrl.setVisibility(isSerie ? View.GONE : View.VISIBLE);
                etDuration.setVisibility(isTrailer || isSerie ? View.GONE : View.VISIBLE);
                etDirector.setVisibility(isTrailer ? View.GONE : View.VISIBLE);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnAddEpisode.setOnClickListener(v -> addEpisodeToList());
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Verificar si es modo edición
        checkEditMode(typeAdapter, genreAdapter);

        btnSave.setOnClickListener(v -> saveContent());
    }

    private void addEpisodeToList() {
        String seasonStr = etEpSeason.getText().toString().trim();
        String epNumStr = etEpNumber.getText().toString().trim();
        String epTitle = etEpTitle.getText().toString().trim();
        String epUrl = etEpVideoUrl.getText().toString().trim();
        String epDuration = etEpDuration.getText().toString().trim();
        String epImgUrl = etEpImageUrl.getText().toString().trim();

        if (epTitle.isEmpty() || epUrl.isEmpty()) {
            Toast.makeText(this, "Ingresa el título y URL del video del capítulo", Toast.LENGTH_SHORT).show();
            return;
        }

        int season = seasonStr.isEmpty() ? 1 : Integer.parseInt(seasonStr);
        int epNum = epNumStr.isEmpty() ? (addedEpisodes.size() + 1) : Integer.parseInt(epNumStr);

        Episode ep = new Episode(season, epNum, epTitle, epUrl, epDuration.isEmpty() ? "45m" : epDuration, epImgUrl);

        if (editingEpisodeIndex >= 0 && editingEpisodeIndex < addedEpisodes.size()) {
            addedEpisodes.set(editingEpisodeIndex, ep);
            Toast.makeText(this, "Capítulo actualizado", Toast.LENGTH_SHORT).show();
        } else {
            addedEpisodes.add(ep);
            Toast.makeText(this, "Capítulo agregado", Toast.LENGTH_SHORT).show();
        }

        episodeAdapter.notifyDataSetChanged();
        resetEpisodeForm();
    }

    private void resetEpisodeForm() {
        editingEpisodeIndex = -1;
        etEpTitle.setText("");
        etEpVideoUrl.setText("");
        etEpDuration.setText("");
        etEpImageUrl.setText("");
        etEpNumber.setText(String.valueOf(addedEpisodes.size() + 1));
        btnAddEpisode.setText("＋ Agregar Capítulo");
    }

    private void checkEditMode(ArrayAdapter<String> typeAdapter, ArrayAdapter<String> genreAdapter) {
        if (getIntent().hasExtra("id")) {
            isEditMode = true;
            existingId = getIntent().getStringExtra("id");

            TextView tvAddTitle = findViewById(R.id.tvAddTitle);
            if (tvAddTitle != null) {
                tvAddTitle.setText("Editar contenido");
            }
            btnSave.setText("Guardar cambios");

            etTitle.setText(getIntent().getStringExtra("title"));
            etYear.setText(String.valueOf(getIntent().getIntExtra("year", 2024)));
            existingRating = getIntent().getDoubleExtra("rating", 0.0);
            etDuration.setText(getIntent().getStringExtra("duration"));
            etDescription.setText(getIntent().getStringExtra("description"));
            etDirector.setText(getIntent().getStringExtra("director"));
            
            String imgUrl = getIntent().getStringExtra("imageUrl");
            etImageUrl.setText(imgUrl);
            etVideoUrl.setText(getIntent().getStringExtra("videoUrl"));

            if (imgUrl != null && !imgUrl.isEmpty()) {
                Glide.with(this).load(imgUrl).into(ivPosterPreview);
            }

            String type = getIntent().getStringExtra("type");
            if (type != null) {
                int pos = typeAdapter.getPosition(type);
                if (pos >= 0) spType.setSelection(pos);
            }

            String genre = getIntent().getStringExtra("genre");
            if (genre != null) {
                int pos = genreAdapter.getPosition(genre);
                if (pos >= 0) spGenre.setSelection(pos);
            }

            // Cargar datos existentes desde Firebase para preservar estrenos y cines
            FirebaseService.getDatabaseRef().child("catalog").child(existingId)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot snapshot) {
                            MovieContent existingContent = snapshot.getValue(MovieContent.class);
                            if (existingContent != null) {
                                cbProcessing.setChecked(existingContent.isProcessing());
                                existingRating = existingContent.getRating();
                                existingMonthlyPremiere = existingContent.isMonthlyPremiere();
                                existingAlliedCinemasEnabled = existingContent.isAlliedCinemasEnabled();
                                existingExpirationTimestamp = existingContent.getPremiereExpirationTimestamp();
                                if (existingContent.getAlliedCinemas() != null) {
                                    existingAlliedCinemas.clear();
                                    existingAlliedCinemas.addAll(existingContent.getAlliedCinemas());
                                }

                                if (existingContent.getEpisodes() != null) {
                                    addedEpisodes.clear();
                                    addedEpisodes.addAll(existingContent.getEpisodes());
                                    episodeAdapter.notifyDataSetChanged();
                                }
                            }
                        }
                        @Override
                        public void onCancelled(DatabaseError error) {}
                    });
        }
    }

    private void saveContent() {
        String title = etTitle.getText().toString().trim();
        String yearStr = etYear.getText().toString().trim();
        String duration = etDuration.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String director = etDirector.getText().toString().trim();
        String imageUrlInput = etImageUrl.getText().toString().trim();
        String videoUrl = etVideoUrl.getText().toString().trim();
        String type = spType.getSelectedItem().toString();
        String genre = spGenre.getSelectedItem().toString();

        if (title.isEmpty() || yearStr.isEmpty()) {
            Toast.makeText(this, "Completa los campos obligatorios", Toast.LENGTH_SHORT).show();
            return;
        }

        int year = Integer.parseInt(yearStr);
        double rating = isEditMode ? existingRating : 0.0;

        btnSave.setEnabled(false);
        saveToDatabase(title, type, genre, year, rating, duration, description, director, imageUrlInput, imageUrlInput, videoUrl);
    }

    private void saveToDatabase(String title, String type, String genre, int year, double rating,
                                String duration, String description, String director,
                                String imageUrl, String bannerUrl, String videoUrl) {
        MovieContent content = new MovieContent(title, type, genre, year, rating, duration, description, director, imageUrl, bannerUrl, videoUrl);
        if (isEditMode && existingId != null) {
            content.setId(existingId);
        }

        content.setProcessing(cbProcessing.isChecked());
        // Preservar configuración de estreno y cines previamente asignada
        content.setMonthlyPremiere(existingMonthlyPremiere);
        content.setAlliedCinemasEnabled(existingAlliedCinemasEnabled);
        content.setPremiereExpirationTimestamp(existingExpirationTimestamp);
        content.setAlliedCinemas(existingAlliedCinemas);

        if ("Serie".equalsIgnoreCase(type)) {
            content.setEpisodes(addedEpisodes);
        }

        FirebaseService.getDatabaseRef().child("catalog")
                .child(content.getId())
                .setValue(content)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, isEditMode ? "Contenido actualizado exitosamente" : "Contenido guardado exitosamente", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnSave.setEnabled(true);
                    Toast.makeText(this, "Error al guardar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
