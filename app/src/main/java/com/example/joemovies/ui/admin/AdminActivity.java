package com.example.joemovies.ui.admin;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.joemovies.R;
import com.example.joemovies.data.FirebaseService;
import com.example.joemovies.data.model.MovieContent;
import com.example.joemovies.ui.adapter.AdminMovieAdapter;
import com.example.joemovies.ui.auth.LoginActivity;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class AdminActivity extends AppCompatActivity {

    private TextView tvTotalTitles, tvTotalMovies;
    private RecyclerView rvAdminCatalog;
    private AdminMovieAdapter adminAdapter;
    private List<MovieContent> movieList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        tvTotalTitles = findViewById(R.id.tvTotalTitles);
        tvTotalMovies = findViewById(R.id.tvTotalMovies);
        rvAdminCatalog = findViewById(R.id.rvAdminCatalog);

        movieList = new ArrayList<>();
        adminAdapter = new AdminMovieAdapter(movieList);
        rvAdminCatalog.setAdapter(adminAdapter);

        Button btnLogout = findViewById(R.id.btnAdminLogout);
        btnLogout.setOnClickListener(v -> {
            FirebaseService.getAuth().signOut();
            startActivity(new Intent(AdminActivity.this, LoginActivity.class));
            finish();
        });

        findViewById(R.id.fabAddContent).setOnClickListener(v -> {
            startActivity(new Intent(AdminActivity.this, AddContentActivity.class));
        });

        Button btnManagePremieres = findViewById(R.id.btnManagePremieres);
        if (btnManagePremieres != null) {
            btnManagePremieres.setOnClickListener(v -> {
                startActivity(new Intent(AdminActivity.this, ManagePremieresActivity.class));
            });
        }

        loadAdminCatalog();
    }

    private void loadAdminCatalog() {
        FirebaseService.getDatabaseRef().child("catalog")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        movieList.clear();
                        int peliculasCount = 0;

                        for (DataSnapshot child : snapshot.getChildren()) {
                            MovieContent movie = child.getValue(MovieContent.class);
                            if (movie != null) {
                                movieList.add(movie);
                                peliculasCount++;
                            }
                        }

                        adminAdapter.notifyDataSetChanged();

                        if (tvTotalTitles != null) {
                            tvTotalTitles.setText(String.valueOf(movieList.size()));
                        }
                        if (tvTotalMovies != null) {
                            tvTotalMovies.setText(String.valueOf(peliculasCount));
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(AdminActivity.this, "Error al cargar catálogo: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
