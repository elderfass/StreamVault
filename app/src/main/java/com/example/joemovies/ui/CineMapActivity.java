package com.example.joemovies.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.joemovies.R;
import com.example.joemovies.data.FirebaseService;
import com.example.joemovies.data.model.Cinema;
import com.example.joemovies.data.model.MovieContent;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CineMapActivity extends AppCompatActivity {

    public static final String EXTRA_MOVIE_ID = "extra_movie_id";
    public static final String EXTRA_MOVIE_TITLE = "extra_movie_title";
    private static final String TAG = "CineMapActivity";

    private WebView webViewMap;
    private TextView tvMovieTitleHeader;
    private ImageButton btnBack;

    private String movieId;
    private String movieTitle;
    private boolean isPageLoaded = false;
    private final List<Cinema> cinemaList = new ArrayList<>();

    // "Cómo llegar"
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private double pendLat, pendLng;
    private String pendName = "";

    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean granted = Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_FINE_LOCATION))
                        || Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_COARSE_LOCATION));
                if (granted) {
                    obtenerUbicacionYRuta(pendLat, pendLng, pendName);
                } else {
                    Toast.makeText(this, "Sin permiso de ubicación. Abriendo Google Maps…", Toast.LENGTH_LONG).show();
                    abrirNavegacionExterna(pendLat, pendLng);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cine_map);

        movieId = getIntent().getStringExtra(EXTRA_MOVIE_ID);
        movieTitle = getIntent().getStringExtra(EXTRA_MOVIE_TITLE);
        if (movieTitle == null || movieTitle.isEmpty()) movieTitle = "Estreno General";

        webViewMap = findViewById(R.id.webViewMap);
        tvMovieTitleHeader = findViewById(R.id.tvMovieTitleHeader);
        btnBack = findViewById(R.id.btnBack);

        tvMovieTitleHeader.setText("Película: " + movieTitle);
        btnBack.setOnClickListener(v -> finish());

        setupWebView();
        fetchCinemasFromDatabase();
    }

    @SuppressLint({"SetJavaScriptEnabled", "JavascriptInterface"})
    private void setupWebView() {
        WebSettings s = webViewMap.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(false); // file:///android_asset/ sigue funcionando

        webViewMap.addJavascriptInterface(new MapBridge(), "Android");
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

    /** Puente JS -> Java. Se ejecuta en un hilo del WebView, por eso runOnUiThread. */
    private class MapBridge {
        @JavascriptInterface
        public void comoLlegar(double lat, double lng, String name) {
            runOnUiThread(() -> iniciarComoLlegar(lat, lng, name));
        }

        @JavascriptInterface
        public void abrirNavegacion(double lat, double lng) {
            runOnUiThread(() -> abrirNavegacionExterna(lat, lng));
        }
    }

    // ---------- Cómo llegar ----------

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void iniciarComoLlegar(double lat, double lng, String name) {
        pendLat = lat;
        pendLng = lng;
        pendName = name;
        if (hasLocationPermission()) {
            obtenerUbicacionYRuta(lat, lng, name);
        } else {
            permissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION});
        }
    }

    @SuppressLint("MissingPermission") // verificado en hasLocationPermission()
    private void obtenerUbicacionYRuta(double dLat, double dLng, String name) {
        if (!hasLocationPermission()) return;
        Toast.makeText(this, "Obteniendo tu ubicación…", Toast.LENGTH_SHORT).show();

        FusedLocationProviderClient fused = LocationServices.getFusedLocationProviderClient(this);
        CancellationTokenSource cts = new CancellationTokenSource();

        fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.getToken())
                .addOnSuccessListener(loc -> {
                    if (loc != null) {
                        calcularRuta(loc, dLat, dLng, name);
                    } else {
                        fused.getLastLocation().addOnSuccessListener(last -> {
                            if (last != null) calcularRuta(last, dLat, dLng, name);
                            else ubicacionNoDisponible(dLat, dLng);
                        });
                    }
                })
                .addOnFailureListener(e -> ubicacionNoDisponible(dLat, dLng));
    }

    private void ubicacionNoDisponible(double dLat, double dLng) {
        if (isFinishing() || isDestroyed()) return;
        Toast.makeText(this, "No se pudo obtener tu ubicación. Activa el GPS. Abriendo Google Maps…",
                Toast.LENGTH_LONG).show();
        abrirNavegacionExterna(dLat, dLng);
    }

    /** Ruta en auto vía OSRM (OpenStreetMap). Devuelve geometría GeoJSON + distancia + duración. */
    private void calcularRuta(Location from, double dLat, double dLng, String name) {
        final double uLat = from.getLatitude();
        final double uLng = from.getLongitude();

        io.execute(() -> {
            HttpURLConnection conn = null;
            try {
                String url = String.format(Locale.US,
                        "https://router.project-osrm.org/route/v1/driving/%f,%f;%f,%f?overview=full&geometries=geojson",
                        uLng, uLat, dLng, dLat);
                conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setRequestProperty("User-Agent", "JoeMovies/1.0 (contacto@tudominio.com)");
                conn.setConnectTimeout(12000);
                conn.setReadTimeout(12000);

                StringBuilder sb = new StringBuilder();
                try (BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    String line;
                    while ((line = r.readLine()) != null) sb.append(line);
                }

                JSONObject root = new JSONObject(sb.toString());
                if (!"Ok".equals(root.optString("code"))) throw new Exception("Sin ruta disponible");
                JSONObject route = root.getJSONArray("routes").getJSONObject(0);

                final String js = "mostrarRuta(" + uLat + "," + uLng + ","
                        + route.getJSONObject("geometry") + ","
                        + route.getDouble("distance") + ","
                        + route.getDouble("duration") + ","
                        + JSONObject.quote(name) + ");";

                runOnUiThread(() -> {
                    if (!isFinishing() && !isDestroyed()) webViewMap.evaluateJavascript(js, null);
                });
            } catch (Exception e) {
                Log.e(TAG, "Error calculando ruta", e);
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    Toast.makeText(this, "No se pudo calcular la ruta. Abriendo Google Maps…",
                            Toast.LENGTH_LONG).show();
                    abrirNavegacionExterna(dLat, dLng);
                });
            } finally {
                if (conn != null) conn.disconnect();
            }
        });
    }

    /** Navegación paso a paso en Google Maps (no requiere permisos en esta app). */
    private void abrirNavegacionExterna(double lat, double lng) {
        try {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=" + lat + "," + lng));
            i.setPackage("com.google.android.apps.maps");
            startActivity(i);
        } catch (ActivityNotFoundException e) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(
                    "https://www.google.com/maps/dir/?api=1&destination=" + lat + "," + lng)));
        }
    }

    // ---------- Datos de cines (sin cambios) ----------

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
                            if (cinemaList.isEmpty()) fetchGlobalCinemas();
                            else if (isPageLoaded) sendCinemasToLeafletMap();
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
                            if (cinema != null && (movieId == null || movieId.equals(cinema.getMovieId())
                                    || cinema.getMovieId() == null)) {
                                cinemaList.add(cinema);
                            }
                        }
                        if (cinemaList.isEmpty()) loadDemoCinemas();
                        if (isPageLoaded) sendCinemasToLeafletMap();
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Log.e(TAG, "Error al cargar cines globales: " + error.getMessage());
                        loadDemoCinemas();
                        if (isPageLoaded) sendCinemasToLeafletMap();
                    }
                });
    }

    private void loadDemoCinemas() {
        cinemaList.clear();
        cinemaList.add(new Cinema("Cinemark Aguachica", movieId, movieTitle, "14:30, 17:15, 20:00",
                8.3075, -73.6180, "Calle 5 # 10-25, Aguachica, Cesar"));
        cinemaList.add(new Cinema("Royal Films Mayales", movieId, movieTitle, "15:00, 18:30, 21:15",
                10.4631, -73.2532, "C.C. Mayales Plaza, Valledupar"));
        cinemaList.add(new Cinema("Cine Colombia Cacique", movieId, movieTitle, "13:00, 16:00, 19:30, 22:00",
                7.1139, -73.1198, "C.C. Cacique, Bucaramanga"));
        cinemaList.add(new Cinema("Procinal Plaza Central", movieId, movieTitle, "14:00, 17:00, 20:30",
                4.6280, -74.1140, "C.C. Plaza Central, Bogotá"));
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
            webViewMap.evaluateJavascript("cargarCines(" + jsonArray + ");",
                    value -> Log.d(TAG, "Leaflet JS ejecutado: " + value));
        } catch (Exception e) {
            Log.e(TAG, "Error al convertir cines a JSON", e);
            Toast.makeText(this, "Error al mostrar cines en el mapa", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        io.shutdownNow();
        if (webViewMap != null) {
            webViewMap.removeJavascriptInterface("Android");
            webViewMap.destroy();
        }
        super.onDestroy();
    }
}
