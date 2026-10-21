package com.example.joemovies.ui.admin;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.joemovies.R;
import com.example.joemovies.data.FirebaseService;
import com.example.joemovies.data.model.Cinema;
import com.example.joemovies.data.model.MovieContent;
import com.example.joemovies.utils.GeocodingHelper;

import java.util.ArrayList;
import java.util.List;

public class ManagePremieresActivity extends AppCompatActivity {

    private EditText etTrailerTitle, etTrailerDescription, etTrailerImageUrl, etTrailerVideoUrl, etDurationDays;
    private CheckBox cbEnableCinemas;
    private LinearLayout llCinemaForm;
    private EditText etCinemaName, etCinemaAddress, etCinemaShowtimes;
    private Button btnAddCinema, btnSavePremiere;
    private TextView tvCinemasSummary;

    private final List<Cinema> currentCinemas = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_premieres);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        etTrailerTitle = findViewById(R.id.etTrailerTitle);
        etTrailerDescription = findViewById(R.id.etTrailerDescription);
        etTrailerImageUrl = findViewById(R.id.etTrailerImageUrl);
        etTrailerVideoUrl = findViewById(R.id.etTrailerVideoUrl);
        etDurationDays = findViewById(R.id.etDurationDays);
        cbEnableCinemas = findViewById(R.id.cbEnableCinemas);
        llCinemaForm = findViewById(R.id.llCinemaForm);
        etCinemaName = findViewById(R.id.etCinemaName);
        etCinemaAddress = findViewById(R.id.etCinemaAddress);
        etCinemaShowtimes = findViewById(R.id.etCinemaShowtimes);
        btnAddCinema = findViewById(R.id.btnAddCinema);
        btnSavePremiere = findViewById(R.id.btnSavePremiere);
        tvCinemasSummary = findViewById(R.id.tvCinemasSummary);

        cbEnableCinemas.setOnCheckedChangeListener((buttonView, isChecked) ->
                llCinemaForm.setVisibility(isChecked ? View.VISIBLE : View.GONE));

        btnAddCinema.setOnClickListener(v -> addCinema());
        btnSavePremiere.setOnClickListener(v -> savePremiereTrailer());
    }

    private void addCinema() {
        final String name = etCinemaName.getText().toString().trim();
        final String address = etCinemaAddress.getText().toString().trim();
        String st = etCinemaShowtimes.getText().toString().trim();
        final String showtimes = st.isEmpty() ? "15:00, 18:30, 21:00" : st;

        if (name.isEmpty() || address.isEmpty()) {
            Toast.makeText(this, "Ingresa el nombre y la dirección del cine", Toast.LENGTH_SHORT).show();
            return;
        }

        btnAddCinema.setEnabled(false);
        btnAddCinema.setText("Buscando ubicación...");

        // 1) solo la dirección; 2) si falla, nombre + dirección
        GeocodingHelper.geocode(address, new GeocodingHelper.Callback() {
            @Override
            public void onResult(double lat, double lng) {
                onLocated(name, address, showtimes, lat, lng);
            }

            @Override
            public void onError(String message) {
                if (isFinishing() || isDestroyed()) return;
                GeocodingHelper.geocode(name + ", " + address, new GeocodingHelper.Callback() {
                    @Override
                    public void onResult(double lat, double lng) {
                        onLocated(name, address, showtimes, lat, lng);
                    }

                    @Override
                    public void onError(String msg2) {
                        if (isFinishing() || isDestroyed()) return;
                        resetAddButton();
                        Toast.makeText(ManagePremieresActivity.this,
                                "No se encontró la dirección. Incluye la ciudad, p. ej. \"Calle 5 # 10-25, Aguachica\". (" + msg2 + ")",
                                Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    private void onLocated(String name, String address, String showtimes, double lat, double lng) {
        if (isFinishing() || isDestroyed()) return;

        currentCinemas.add(new Cinema(
                name, "", etTrailerTitle.getText().toString().trim(),
                showtimes, lat, lng, address));

        tvCinemasSummary.setText("Cines añadidos: " + currentCinemas.size());
        Toast.makeText(this, String.format(java.util.Locale.US,
                "Cine '%s' añadido (%.4f, %.4f)", name, lat, lng), Toast.LENGTH_SHORT).show();

        etCinemaName.setText("");
        etCinemaAddress.setText("");
        etCinemaShowtimes.setText("");
        resetAddButton();
    }

    private void resetAddButton() {
        btnAddCinema.setEnabled(true);
        btnAddCinema.setText("Añadir cine");
    }

    private void savePremiereTrailer() {
        String title = etTrailerTitle.getText().toString().trim();
        String description = etTrailerDescription.getText().toString().trim();
        String imageUrl = etTrailerImageUrl.getText().toString().trim();
        String videoUrl = etTrailerVideoUrl.getText().toString().trim();
        String daysStr = etDurationDays.getText().toString().trim();

        if (title.isEmpty() || videoUrl.isEmpty()) {
            Toast.makeText(this, "Ingresa el título y la URL del video del tráiler", Toast.LENGTH_SHORT).show();
            return;
        }

        int days;
        try {
            days = daysStr.isEmpty() ? 14 : Integer.parseInt(daysStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Duración en días no válida", Toast.LENGTH_SHORT).show();
            return;
        }
        long expirationMs = System.currentTimeMillis() + (days * 24L * 60L * 60L * 1000L);
        boolean enableCinemas = cbEnableCinemas.isChecked();

        MovieContent trailer = new MovieContent(
                title, "Trailer", "Estreno", 2026, 9.0, "2m",
                description, "Director", imageUrl, imageUrl, videoUrl);

        trailer.setMonthlyPremiere(true);
        trailer.setAlliedCinemasEnabled(enableCinemas);
        trailer.setPremiereExpirationTimestamp(expirationMs);
        trailer.setAlliedCinemas(currentCinemas);

        btnSavePremiere.setEnabled(false);

        if (enableCinemas && !currentCinemas.isEmpty()) {
            for (Cinema c : currentCinemas) {
                c.setMovieId(trailer.getId());
                c.setMovieTitle(trailer.getTitle());
                FirebaseService.getDatabaseRef().child("cinemas").child(c.getId()).setValue(c);
            }
        }

        FirebaseService.getDatabaseRef().child("catalog").child(trailer.getId()).setValue(trailer)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "🎬 Tráiler y estreno publicado exitosamente", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnSavePremiere.setEnabled(true);
                    Toast.makeText(this, "Error al publicar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}