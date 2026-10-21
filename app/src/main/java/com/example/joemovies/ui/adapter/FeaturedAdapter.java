package com.example.joemovies.ui.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.joemovies.R;
import com.example.joemovies.data.model.MovieContent;
import com.example.joemovies.ui.DetailActivity;
import com.example.joemovies.utils.ImageUtils;

import java.util.List;

public class FeaturedAdapter extends RecyclerView.Adapter<FeaturedAdapter.FeaturedViewHolder> {

    private final List<MovieContent> movieList;

    public FeaturedAdapter(List<MovieContent> movieList) {
        this.movieList = movieList;
    }

    @NonNull
    @Override
    public FeaturedViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_featured_movie, parent, false);
        return new FeaturedViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FeaturedViewHolder holder, int position) {
        MovieContent movie = movieList.get(position);
        Context context = holder.itemView.getContext();

        holder.tvTitle.setText(movie.getTitle());
        holder.tvType.setText(movie.getType() != null ? movie.getType().toUpperCase() : "NOVEDAD");
        String ratingStr = movie.getRating() <= 0.0 ? "-" : String.valueOf(movie.getRating());
        holder.tvMeta.setText("⭐ " + ratingStr + " • " + (movie.getGenre() != null ? movie.getGenre() : "") + " • " + movie.getYear());

        String displayImg = (movie.getBannerUrl() != null && !movie.getBannerUrl().isEmpty()) ? movie.getBannerUrl() : movie.getImageUrl();

        if (displayImg != null && !displayImg.isEmpty()) {
            Glide.with(context)
                    .load(ImageUtils.getDirectImageUrl(displayImg))
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_gallery)
                    .into(holder.ivBanner);
        } else {
            holder.ivBanner.setImageResource(android.R.drawable.ic_menu_gallery);
        }

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
    }

    @Override
    public int getItemCount() {
        return movieList.size();
    }

    public static class FeaturedViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvType, tvMeta;
        ImageView ivBanner;

        public FeaturedViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvFeaturedTitle);
            tvType = itemView.findViewById(R.id.tvFeaturedType);
            tvMeta = itemView.findViewById(R.id.tvFeaturedMeta);
            ivBanner = itemView.findViewById(R.id.ivFeaturedBanner);
        }
    }
}
