package com.example.joemovies.utils;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Dirección -> coordenadas usando Nominatim (OpenStreetMap). Callbacks en el hilo principal. */
public final class GeocodingHelper {

    public interface Callback {
        void onResult(double lat, double lng);
        void onError(String message);
    }

    private static final ExecutorService EXEC = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private GeocodingHelper() {}

    public static void geocode(String query, Callback cb) {
        EXEC.execute(() -> {
            HttpURLConnection conn = null;
            try {
                String url = "https://nominatim.openstreetmap.org/search?format=jsonv2&limit=1"
                        + "&countrycodes=co&q=" + URLEncoder.encode(query, "UTF-8");
                conn = (HttpURLConnection) new URL(url).openConnection();
                // Nominatim exige un User-Agent identificable (cambia el correo por el tuyo)
                conn.setRequestProperty("User-Agent", "JoeMovies/1.0 (contacto@tudominio.com)");
                conn.setRequestProperty("Accept-Language", "es");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                if (conn.getResponseCode() != 200) {
                    postError(cb, "Servicio de mapas no disponible (" + conn.getResponseCode() + ")");
                    return;
                }

                StringBuilder sb = new StringBuilder();
                try (BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    String line;
                    while ((line = r.readLine()) != null) sb.append(line);
                }

                JSONArray arr = new JSONArray(sb.toString());
                if (arr.length() == 0) {
                    postError(cb, "Dirección no encontrada");
                    return;
                }
                JSONObject o = arr.getJSONObject(0);
                final double lat = Double.parseDouble(o.getString("lat"));
                final double lng = Double.parseDouble(o.getString("lon"));
                MAIN.post(() -> cb.onResult(lat, lng));
            } catch (Exception e) {
                postError(cb, "Error de red: " + e.getMessage());
            } finally {
                if (conn != null) conn.disconnect();
            }
        });
    }

    private static void postError(Callback cb, String msg) {
        MAIN.post(() -> cb.onError(msg));
    }
}
