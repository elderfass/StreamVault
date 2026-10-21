package com.example.joemovies.ui.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.joemovies.R;
import com.example.joemovies.data.FirebaseService;
import com.example.joemovies.data.model.MovieContent;
import com.example.joemovies.ui.DetailActivity;
import com.example.joemovies.ui.admin.AddContentActivity;

import java.util.List;

public class AdminMovieAdapter extends RecyclerView.Adapter<AdminMovieAdapter.AdminViewHolder> {

    private final List<MovieContent> movieList;

    public AdminMovieAdapter(List<MovieContent> movieList) {
        this.movieList = movieList;
    }

    @NonNull
    @Override
    public AdminViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_movie, parent, false);
        return new AdminViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AdminViewHolder holder, int position) {
        MovieContent movie = movieList.get(position);
        Context context = holder.itemView.getContext();

        holder.tvTitle.setText(movie.getTitle());
        holder.tvGenre.setText((movie.getGenre() != null ? movie.getGenre() : "") + " • " + movie.getYear());
        holder.tvRating.setText("⭐ " + movie.getRating());

        if (movie.getImageUrl() != null && !movie.getImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(com.example.joemovies.utils.ImageUtils.getDirectImageUrl(movie.getImageUrl()))
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_gallery)
                    .into(holder.ivPoster);
        } else {
            holder.ivPoster.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        // Click en la tarjeta -> Ver detalle
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, DetailActivity.class);
            intent.putExtra("id", movie.getId());
            intent.putExtra("title", movie.getTitle());
            intent.putExtra("type", movie.getType());
            intent.putExtra("genre", movie.getGenre());
            intent.putExtra("description", movie.getDescription());
            intent.putExtra("director", movie.getDirector());
            intent.putExtra("duration", movie.getDuration());
            intent.putExtra("year", movie.getYear());
            intent.putExtra("rating", movie.getRating());
            intent.putExtra("imageUrl", movie.getImageUrl());
            intent.putExtra("bannerUrl", movie.getBannerUrl());
            intent.putExtra("videoUrl", movie.getVideoUrl());
            context.startActivity(intent);
        });

        // Click en Editar
        holder.btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(context, AddContentActivity.class);
            intent.putExtra("id", movie.getId());
            intent.putExtra("title", movie.getTitle());
            intent.putExtra("type", movie.getType());
            intent.putExtra("genre", movie.getGenre());
            intent.putExtra("description", movie.getDescription());
            intent.putExtra("director", movie.getDirector());
            intent.putExtra("duration", movie.getDuration());
            intent.putExtra("year", movie.getYear());
            intent.putExtra("rating", movie.getRating());
            intent.putExtra("imageUrl", movie.getImageUrl());
            intent.putExtra("bannerUrl", movie.getBannerUrl());
            intent.putExtra("videoUrl", movie.getVideoUrl());
            context.startActivity(intent);
        });

        // Click en Eliminar
        holder.btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("Eliminar película")
                    .setMessage("¿Estás seguro de que deseas eliminar '" + movie.getTitle() + "' del catálogo?")
                    .setPositiveButton("Eliminar", (dialog, which) -> {
                        FirebaseService.getDatabaseRef().child("catalog").child(movie.getId()).removeValue()
                                .addOnSuccessListener(aVoid -> Toast.makeText(context, "Eliminado exitosamente", Toast.LENGTH_SHORT).show())
                                .addOnFailureListener(e -> Toast.makeText(context, "Error al eliminar: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        });
    }

    @Override
    public int getItemCount() {
        return movieList.size();
    }

    public static class AdminViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvGenre, tvRating;
        ImageView ivPoster;
        ImageButton btnEdit, btnDelete;

        public AdminViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvAdminMovieTitle);
            tvGenre = itemView.findViewById(R.id.tvAdminMovieGenre);
            tvRating = itemView.findViewById(R.id.tvAdminMovieRating);
            ivPoster = itemView.findViewById(R.id.ivAdminPoster);
            btnEdit = itemView.findViewById(R.id.btnAdminEdit);
            btnDelete = itemView.findViewById(R.id.btnAdminDelete);
        }
    }
}
