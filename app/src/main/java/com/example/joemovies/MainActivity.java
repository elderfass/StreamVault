package com.example.joemovies;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.joemovies.data.FirebaseService;
import com.example.joemovies.data.SessionManager;
import com.example.joemovies.data.model.MovieContent;
import com.example.joemovies.ui.CineMapActivity;
import com.example.joemovies.ui.adapter.FeaturedAdapter;
import com.example.joemovies.ui.adapter.MovieAdapter;
import com.example.joemovies.ui.adapter.PremiereAdapter;
import com.example.joemovies.ui.auth.LoginActivity;
import com.example.joemovies.ui.player.PlayerActivity;
import com.example.joemovies.utils.VideoUrlResolver;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private View layoutHome, layoutSearch, layoutProfile, llPremieresSection;
    private TextView tvProfileName, tvProfileEmail;
    private RecyclerView rvCatalog, rvFeatured, rvSearchResults, rvMonthlyPremieres;
    private MovieAdapter movieAdapter, searchAdapter;
    private FeaturedAdapter featuredAdapter;
    private PremiereAdapter premiereAdapter;
    private List<MovieContent> movieContentList, featuredList, searchList, premiereList;
    private EditText etSearch;
    private Spinner spSearchGenre, spSearchRating;

    private final Handler carouselHandler = new Handler(Looper.getMainLooper());
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private final Runnable searchRunnable = this::applySearchFilters;
    private int premierePos = 0, featuredPos = 0;
    private boolean hasShownPopupThisSession = false;

    private DatabaseReference catalogRef;
    private ValueEventListener catalogListener;
    private Dialog premiereDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Verificación de sesión al inicio
        FirebaseUser user = FirebaseService.getAuth().getCurrentUser();
        if (user == null) {
            navigateToLogin();
            return;
        }

        layoutHome = findViewById(R.id.layoutHome);
        layoutSearch = findViewById(R.id.layoutSearch);
        layoutProfile = findViewById(R.id.layoutProfile);
        tvProfileName = findViewById(R.id.tvProfileName);
        tvProfileEmail = findViewById(R.id.tvProfileEmail);
        rvCatalog = findViewById(R.id.rvCatalog);
        rvFeatured = findViewById(R.id.rvFeatured);
        rvSearchResults = findViewById(R.id.rvSearchResults);
        rvMonthlyPremieres = findViewById(R.id.rvMonthlyPremieres);
        llPremieresSection = findViewById(R.id.llPremieresSection);
        etSearch = findViewById(R.id.etSearch);
        spSearchGenre = findViewById(R.id.spSearchGenre);
        spSearchRating = findViewById(R.id.spSearchRating);

        movieContentList = new ArrayList<>();
        movieAdapter = new MovieAdapter(movieContentList);
        rvCatalog.setAdapter(movieAdapter);

        featuredList = new ArrayList<>();
        featuredAdapter = new FeaturedAdapter(featuredList);
        rvFeatured.setAdapter(featuredAdapter);

        premiereList = new ArrayList<>();
        premiereAdapter = new PremiereAdapter(premiereList);
        rvMonthlyPremieres.setAdapter(premiereAdapter);

        searchList = new ArrayList<>();
        searchAdapter = new MovieAdapter(searchList);
        rvSearchResults.setAdapter(searchAdapter);

        setupSearchFilters();

        findViewById(R.id.btnSettings).setOnClickListener(v -> toggleTheme());

        tvProfileEmail.setText(user.getEmail());
        fetchUserProfile(user.getUid());
        loadCatalog();

        BottomNavigationView nav = findViewById(R.id.bottomNavigation);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) { showView(layoutHome); return true; }
            if (id == R.id.nav_search) { showView(layoutSearch); return true; }
            if (id == R.id.nav_profile) { showView(layoutProfile); return true; }
            return false;
        });

        findViewById(R.id.btnLogout).setOnClickListener(v -> {
            FirebaseService.getAuth().signOut();
            navigateToLogin();
        });
    }

    private void setupSearchFilters() {
        String[] genres = {"Todos los géneros", "Acción", "Sci-Fi", "Drama", "Thriller", "Comedia"};
        ArrayAdapter<String> genreAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, genres);
        genreAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spSearchGenre.setAdapter(genreAdapter);

        String[] ratings = {"Todas las calificaciones", "⭐ 5.0 o más", "⭐ 7.0 o más", "⭐ 8.5 o más", "⭐ 9.5 o más"};
        ArrayAdapter<String> ratingAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, ratings);
        ratingAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spSearchRating.setAdapter(ratingAdapter);

        AdapterView.OnItemSelectedListener filterListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                applySearchFilters();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        };
        spSearchGenre.setOnItemSelectedListener(filterListener);
        spSearchRating.setOnItemSelectedListener(filterListener);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                // debounce 250 ms
                searchHandler.removeCallbacks(searchRunnable);
                searchHandler.postDelayed(searchRunnable, 250);
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void loadCatalog() {
        catalogRef = FirebaseService.getDatabaseRef().child("catalog");
        catalogListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isFinishing() || isDestroyed()) return;

                movieContentList.clear();
                featuredList.clear();
                premiereList.clear();

                for (DataSnapshot itemSnapshot : snapshot.getChildren()) {
                    MovieContent movie = itemSnapshot.getValue(MovieContent.class);
                    if (movie == null) continue;
                    boolean isTrailer = "Trailer".equalsIgnoreCase(movie.getType());

                    if (!isTrailer) {
                        movieContentList.add(movie);
                        featuredList.add(0, movie);
                    }
                    if (isTrailer || movie.isPremiereActive()) {
                        premiereList.add(movie);
                    }
                }

                movieAdapter.notifyDataSetChanged();
                featuredAdapter.notifyDataSetChanged();
                premiereAdapter.notifyDataSetChanged();

                if (llPremieresSection != null) {
                    llPremieresSection.setVisibility(premiereList.isEmpty() ? View.GONE : View.VISIBLE);
                }

                if (!hasShownPopupThisSession && !premiereList.isEmpty()) {
                    hasShownPopupThisSession = true;
                    showPremierePopup(premiereList);
                }

                startCarouselAutoScroll();
                applySearchFilters();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (isFinishing() || isDestroyed()) return;
                Toast.makeText(MainActivity.this, "Error al cargar catálogo: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        };
        catalogRef.addValueEventListener(catalogListener);
    }

    private void showPremierePopup(List<MovieContent> premieres) {
        if (isFinishing() || isDestroyed() || premieres == null || premieres.isEmpty()) return;

        final MovieContent premiere = premieres.get(0);
        final String rawUrl = premiere.getVideoUrl() == null ? "" : premiere.getVideoUrl().trim();

        final Dialog dialog = new Dialog(this);
        premiereDialog = dialog;
        dialog.setContentView(R.layout.dialog_premiere_popup);
        dialog.setCancelable(false); // se cierra solo con "Omitir" tras el contador
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        TextView tvTitle = dialog.findViewById(R.id.tvPopupTitle);
        TextView tvDescription = dialog.findViewById(R.id.tvPopupDescription);
        TextView btnClose = dialog.findViewById(R.id.btnPopupClose);
        Button btnCinemas = dialog.findViewById(R.id.btnPopupCinemas);
        PlayerView playerView = dialog.findViewById(R.id.playerViewPopup);
        ProgressBar pbLoading = dialog.findViewById(R.id.pbTrailerLoading);

        tvTitle.setText(premiere.getTitle());
        tvDescription.setText(premiere.getDescription() != null
                ? premiere.getDescription() : "¡Ya disponible en cines!");

        btnClose.setText("Cargando...");
        btnClose.setEnabled(false);

        final CountDownTimer[] timer = new CountDownTimer[1];
        final ExoPlayer[] player = new ExoPlayer[1];

        // Inicia el contador una sola vez (video listo, error, sin video o timeout)
        final Runnable enableSkip = () -> {
            if (timer[0] != null) return;
            pbLoading.setVisibility(View.GONE);
            timer[0] = new CountDownTimer(10000, 1000) {
                @Override public void onTick(long ms) {
                    btnClose.setText("Omitir (" + ((ms + 999) / 1000) + "s)");
                }
                @Override public void onFinish() {
                    btnClose.setEnabled(true);
                    btnClose.setText("Omitir ✕");
                }
            }.start();
        };

        // Abre el reproductor completo (con fallback a WebView)
        final Runnable openFullPlayer = () -> {
            dialog.dismiss();
            Intent i = new Intent(MainActivity.this, PlayerActivity.class);
            i.putExtra("videoUrl", rawUrl);
            startActivity(i);
        };

        // Liberación centralizada: cubre botones, back, cierre por Activity, etc.
        dialog.setOnDismissListener(d -> {
            btnClose.removeCallbacks(enableSkip);
            if (timer[0] != null) { timer[0].cancel(); timer[0] = null; }
            playerView.setPlayer(null);
            if (player[0] != null) { player[0].release(); player[0] = null; }
            if (premiereDialog == dialog) premiereDialog = null;
        });

        final String direct = VideoUrlResolver.resolveDirect(rawUrl);

        if (rawUrl.isEmpty()) {
            enableSkip.run();
        } else if (direct == null) {
            // YouTube, páginas web, etc.: ExoPlayer no puede -> reproductor completo
            playerView.setUseController(false);
            playerView.setOnClickListener(v -> openFullPlayer.run());
            Toast.makeText(this, "Toca el recuadro para ver el tráiler", Toast.LENGTH_SHORT).show();
            enableSkip.run();
        } else {
            try {
                ExoPlayer exo = new ExoPlayer.Builder(this).build();
                player[0] = exo;
                playerView.setPlayer(exo);
                exo.addListener(new Player.Listener() {
                    @Override
                    public void onPlaybackStateChanged(int state) {
                        if (state == Player.STATE_READY) enableSkip.run();
                    }

                    @Override
                    public void onPlayerError(@NonNull PlaybackException error) {
                        playerView.setUseController(false);
                        playerView.setOnClickListener(v -> openFullPlayer.run());
                        Toast.makeText(MainActivity.this,
                                "No se pudo reproducir aquí. Toca el recuadro para abrir el tráiler.",
                                Toast.LENGTH_LONG).show();
                        enableSkip.run();
                    }
                });
                exo.setMediaSource(VideoUrlResolver.buildMediaSource(this, direct));
                exo.setPlayWhenReady(true);
                exo.prepare();
                // Si nunca llega a READY (red lenta), no bloquear al usuario
                btnClose.postDelayed(enableSkip, 8000);
            } catch (Exception e) {
                enableSkip.run();
            }
        }

        btnClose.setOnClickListener(v -> dialog.dismiss());

        btnCinemas.setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent(MainActivity.this, CineMapActivity.class);
            intent.putExtra(CineMapActivity.EXTRA_MOVIE_ID, premiere.getId());
            intent.putExtra(CineMapActivity.EXTRA_MOVIE_TITLE, premiere.getTitle());
            startActivity(intent);
        });

        dialog.show();
    }

    private void startCarouselAutoScroll() {
        carouselHandler.removeCallbacksAndMessages(null);
        carouselHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!premiereList.isEmpty()) {
                    premierePos = (premierePos + 1) % premiereList.size();
                    rvMonthlyPremieres.smoothScrollToPosition(premierePos);
                } else if (!featuredList.isEmpty()) {
                    featuredPos = (featuredPos + 1) % featuredList.size();
                    rvFeatured.smoothScrollToPosition(featuredPos);
                }
                carouselHandler.postDelayed(this, 4000);
            }
        }, 4000);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (premiereList != null && featuredList != null
                && (!premiereList.isEmpty() || !featuredList.isEmpty())) {
            startCarouselAutoScroll();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        carouselHandler.removeCallbacksAndMessages(null);
        // Evita audio del tráiler en segundo plano y ventanas filtradas
        if (premiereDialog != null && premiereDialog.isShowing()) premiereDialog.dismiss();
    }

    @Override
    protected void onDestroy() {
        carouselHandler.removeCallbacksAndMessages(null);
        searchHandler.removeCallbacksAndMessages(null);
        if (catalogRef != null && catalogListener != null) {
            catalogRef.removeEventListener(catalogListener);
        }
        if (premiereDialog != null && premiereDialog.isShowing()) premiereDialog.dismiss();
        super.onDestroy();
    }

    private void applySearchFilters() {
        if (searchList == null) return;
        String queryText = etSearch.getText().toString().trim().toLowerCase();
        Object sel = spSearchGenre.getSelectedItem();
        String selectedGenre = sel != null ? sel.toString() : "Todos los géneros";
        int ratingPos = spSearchRating.getSelectedItemPosition();

        double minRating = 0.0;
        if (ratingPos == 1) minRating = 5.0;
        else if (ratingPos == 2) minRating = 7.0;
        else if (ratingPos == 3) minRating = 8.5;
        else if (ratingPos == 4) minRating = 9.5;

        List<MovieContent> filtered = new ArrayList<>();
        for (MovieContent item : movieContentList) {
            boolean matchesText = queryText.isEmpty()
                    || (item.getTitle() != null && item.getTitle().toLowerCase().contains(queryText))
                    || (item.getDirector() != null && item.getDirector().toLowerCase().contains(queryText));
            boolean matchesGenre = "Todos los géneros".equalsIgnoreCase(selectedGenre)
                    || (item.getGenre() != null && item.getGenre().equalsIgnoreCase(selectedGenre));
            boolean matchesRating = item.getRating() >= minRating;

            if (matchesText && matchesGenre && matchesRating) filtered.add(item);
        }

        searchList.clear();
        searchList.addAll(filtered);
        searchAdapter.notifyDataSetChanged();
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        SessionManager.updateLastActivity(this);
    }

    private void toggleTheme() {
        boolean dark = AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES;
        AppCompatDelegate.setDefaultNightMode(dark
                ? AppCompatDelegate.MODE_NIGHT_NO : AppCompatDelegate.MODE_NIGHT_YES);
        Toast.makeText(this, dark ? "Modo Claro activado" : "Modo Oscuro activado",
                Toast.LENGTH_SHORT).show();
    }

    private void showView(View viewToShow) {
        layoutHome.setVisibility(View.GONE);
        layoutSearch.setVisibility(View.GONE);
        layoutProfile.setVisibility(View.GONE);
        viewToShow.setVisibility(View.VISIBLE);
    }

    private void fetchUserProfile(String uid) {
        FirebaseService.getDatabaseRef().child("users").child(uid).child("name").get()
                .addOnSuccessListener(snap -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (snap.exists()) tvProfileName.setText(snap.getValue(String.class));
                });
    }

    private void navigateToLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}