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
import com.example.joemovies.ui.CineMapActivity;
import com.example.joemovies.ui.DetailActivity;
import com.example.joemovies.utils.ImageUtils;

import java.util.List;

public class PremiereAdapter extends RecyclerView.Adapter<PremiereAdapter.ViewHolder> {

    private final List<MovieContent> premiereList;

    public PremiereAdapter(List<MovieContent> premiereList) {
        this.premiereList = premiereList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_premiere_movie, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MovieContent item = premiereList.get(position);
        Context context = holder.itemView.getContext();

        holder.tvTitle.setText(item.getTitle());
        holder.tvRating.setText("⭐ " + item.getRating());

        String displayImage = (item.getBannerUrl() != null && !item.getBannerUrl().isEmpty())
                ? item.getBannerUrl() : item.getImageUrl();

        if (displayImage != null && !displayImage.isEmpty()) {
            Glide.with(context)
                    .load(ImageUtils.getDirectImageUrl(displayImage))
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_gallery)
                    .into(holder.ivBackdrop);
        } else {
            holder.ivBackdrop.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, DetailActivity.class);
            intent.putExtra("id", item.getId());
            intent.putExtra("title", item.getTitle());
            intent.putExtra("type", item.getType());
            intent.putExtra("genre", item.getGenre());
            intent.putExtra("description", item.getDescription());
            intent.putExtra("director", item.getDirector());
            intent.putExtra("duration", item.getDuration());
            intent.putExtra("year", item.getYear());
            intent.putExtra("rating", item.getRating());
            intent.putExtra("imageUrl", item.getImageUrl());
            intent.putExtra("bannerUrl", item.getBannerUrl());
            intent.putExtra("videoUrl", item.getVideoUrl());
            context.startActivity(intent);
        });

        holder.btnCinemas.setOnClickListener(v -> {
            Intent intent = new Intent(context, CineMapActivity.class);
            intent.putExtra(CineMapActivity.EXTRA_MOVIE_ID, item.getId());
            intent.putExtra(CineMapActivity.EXTRA_MOVIE_TITLE, item.getTitle());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return premiereList != null ? premiereList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivBackdrop;
        TextView tvTitle, tvRating, btnCinemas;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivBackdrop = itemView.findViewById(R.id.ivPremiereBackdrop);
            tvTitle = itemView.findViewById(R.id.tvPremiereTitle);
            tvRating = itemView.findViewById(R.id.tvPremiereRating);
            btnCinemas = itemView.findViewById(R.id.btnPremiereCinemas);
        }
    }
}
