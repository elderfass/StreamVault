package com.example.joemovies.ui;

import android.os.Bundle;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.joemovies.R;
import com.example.joemovies.data.FirebaseService;
import com.example.joemovies.data.model.Cinema;
import com.example.joemovies.data.model.MovieContent;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class CineMapActivity extends AppCompatActivity {

    public static final String EXTRA_MOVIE_ID = "extra_movie_id";
    public static final String EXTRA_MOVIE_TITLE = "extra_movie_title";

    private WebView webViewMap;
    private TextView tvMovieTitleHeader;
    private ImageButton btnBack;

    private String movieId;
    private String movieTitle;
    private boolean isPageLoaded = false;
    private final List<Cinema> cinemaList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cine_map);

        movieId = getIntent().getStringExtra(EXTRA_MOVIE_ID);
        movieTitle = getIntent().getStringExtra(EXTRA_MOVIE_TITLE);

        if (movieTitle == null || movieTitle.isEmpty()) {
            movieTitle = "Estreno General";
        }

        webViewMap = findViewById(R.id.webViewMap);
        tvMovieTitleHeader = findViewById(R.id.tvMovieTitleHeader);
        btnBack = findViewById(R.id.btnBack);

        tvMovieTitleHeader.setText("Película: " + movieTitle);

        btnBack.setOnClickListener(v -> finish());

        setupWebView();
        fetchCinemasFromDatabase();
    }

    private void setupWebView() {
        WebSettings webSettings = webViewMap.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAllowFileAccess(true);

        webViewMap.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                isPageLoaded = true;
                sendCinemasToLeafletMap();
            }
        });

        webViewMap.loadUrl("file:///android_asset/mapa_cines.html");
    }

    private void fetchCinemasFromDatabase() {
        if (movieId != null && !movieId.isEmpty()) {
            FirebaseService.getDatabaseRef().child("catalog").child(movieId)
                    .addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot snapshot) {
                            cinemaList.clear();
                            MovieContent movie = snapshot.getValue(MovieContent.class);
                            if (movie != null && movie.getAlliedCinemas() != null && !movie.getAlliedCinemas().isEmpty()) {
                                cinemaList.addAll(movie.getAlliedCinemas());
                            }

                            if (cinemaList.isEmpty()) {
                                fetchGlobalCinemas();
                            } else if (isPageLoaded) {
                                sendCinemasToLeafletMap();
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError error) {
                            fetchGlobalCinemas();
                        }
                    });
        } else {
            fetchGlobalCinemas();
        }
    }

    private void fetchGlobalCinemas() {
        FirebaseService.getDatabaseRef().child("cinemas")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        cinemaList.clear();
                        for (DataSnapshot itemSnapshot : snapshot.getChildren()) {
                            Cinema cinema = itemSnapshot.getValue(Cinema.class);
                            if (cinema != null) {
                                if (movieId == null || movieId.equals(cinema.getMovieId()) || cinema.getMovieId() == null) {
                                    cinemaList.add(cinema);
                                }
                            }
                        }

                        if (cinemaList.isEmpty()) {
                            loadDemoCinemas();
                        }

                        if (isPageLoaded) {
                            sendCinemasToLeafletMap();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Log.e("CineMapActivity", "Error al cargar cines globales: " + error.getMessage());
                        loadDemoCinemas();
                        if (isPageLoaded) {
                            sendCinemasToLeafletMap();
                        }
                    }
                });
    }

    private void loadDemoCinemas() {
        cinemaList.clear();
        cinemaList.add(new Cinema(
                "Cinemark Aguachica",
                movieId,
                movieTitle,
                "14:30, 17:15, 20:00",
                8.3075,
                -73.6180,
                "Calle 5 # 10-25, Aguachica, Cesar"
        ));
        cinemaList.add(new Cinema(
                "Royal Films Mayales",
                movieId,
                movieTitle,
                "15:00, 18:30, 21:15",
                10.4631,
                -73.2532,
                "C.C. Mayales Plaza, Valledupar"
        ));
        cinemaList.add(new Cinema(
                "Cine Colombia Cacique",
                movieId,
                movieTitle,
                "13:00, 16:00, 19:30, 22:00",
                7.1139,
                -73.1198,
                "C.C. Cacique, Bucaramanga"
        ));
        cinemaList.add(new Cinema(
                "Procinal Plaza Central",
                movieId,
                movieTitle,
                "14:00, 17:00, 20:30",
                4.6280,
                -74.1140,
                "C.C. Plaza Central, Bogotá"
        ));
    }

    private void sendCinemasToLeafletMap() {
        if (cinemaList.isEmpty()) return;

        try {
            JSONArray jsonArray = new JSONArray();
            for (Cinema cinema : cinemaList) {
                JSONObject obj = new JSONObject();
                obj.put("id", cinema.getId());
                obj.put("nombre", cinema.getName());
                obj.put("pelicula", cinema.getMovieTitle() != null ? cinema.getMovieTitle() : movieTitle);
                obj.put("horarios", cinema.getShowtimes());
                obj.put("lat", cinema.getLatitude());
                obj.put("lng", cinema.getLongitude());
                obj.put("direccion", cinema.getAddress());
                jsonArray.put(obj);
            }

            String jsonString = jsonArray.toString();
            String script = "cargarCines(" + jsonString + ");";

            webViewMap.evaluateJavascript(script, value -> Log.d("CineMapActivity", "Leaflet JS ejecutado: " + value));

        } catch (Exception e) {
            Log.e("CineMapActivity", "Error al convertir cines a JSON", e);
            Toast.makeText(this, "Error al mostrar cines en el mapa", Toast.LENGTH_SHORT).show();
        }
    }
}
