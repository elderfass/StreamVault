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
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.joemovies.R;
import com.example.joemovies.data.model.Episode;
import com.example.joemovies.ui.player.PlayerActivity;
import com.example.joemovies.utils.ImageUtils;

import java.util.List;

public class EpisodeAdapter extends RecyclerView.Adapter<EpisodeAdapter.EpisodeViewHolder> {

    public interface OnEpisodeDeleteListener {
        void onDelete(int position);
    }

    public interface OnEpisodeEditListener {
        void onEdit(int position, Episode episode);
    }

    private final List<Episode> episodeList;
    private final boolean isDeleteMode;
    private final OnEpisodeDeleteListener deleteListener;
    private final OnEpisodeEditListener editListener;

    public EpisodeAdapter(List<Episode> episodeList) {
        this(episodeList, false, null, null);
    }

    public EpisodeAdapter(List<Episode> episodeList, boolean isDeleteMode, OnEpisodeDeleteListener deleteListener, OnEpisodeEditListener editListener) {
        this.episodeList = episodeList;
        this.isDeleteMode = isDeleteMode;
        this.deleteListener = deleteListener;
        this.editListener = editListener;
    }

    @NonNull
    @Override
    public EpisodeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_episode, parent, false);
        return new EpisodeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EpisodeViewHolder holder, int position) {
        Episode episode = episodeList.get(position);
        Context context = holder.itemView.getContext();

        holder.tvTitle.setText("T" + episode.getSeasonNumber() + " E" + episode.getEpisodeNumber() + ": " + episode.getTitle());
        holder.tvInfo.setText("Temporada " + episode.getSeasonNumber() + " • " + (episode.getDuration() != null ? episode.getDuration() : ""));

        if (episode.getImageUrl() != null && !episode.getImageUrl().trim().isEmpty()) {
            Glide.with(context)
                    .load(ImageUtils.getDirectImageUrl(episode.getImageUrl()))
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_gallery)
                    .into(holder.ivEpisodeImage);
        } else {
            holder.ivEpisodeImage.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        if (isDeleteMode) {
            holder.btnDelete.setVisibility(View.VISIBLE);
            holder.btnEdit.setVisibility(View.VISIBLE);

            holder.btnDelete.setOnClickListener(v -> {
                if (deleteListener != null) {
                    deleteListener.onDelete(position);
                }
            });

            View.OnClickListener handleEdit = v -> {
                if (editListener != null) {
                    editListener.onEdit(position, episode);
                }
            };

            holder.btnEdit.setOnClickListener(handleEdit);
            holder.itemView.setOnClickListener(handleEdit);
        } else {
            holder.btnDelete.setVisibility(View.GONE);
            holder.btnEdit.setVisibility(View.GONE);

            holder.itemView.setOnClickListener(v -> {
                if (episode.getVideoUrl() != null && !episode.getVideoUrl().trim().isEmpty()) {
                    Intent intent = new Intent(context, PlayerActivity.class);
                    intent.putExtra("title", episode.getTitle());
                    intent.putExtra("videoUrl", episode.getVideoUrl());
                    context.startActivity(intent);
                } else {
                    Toast.makeText(context, "URL de video no disponible para este capítulo", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return episodeList.size();
    }

    public static class EpisodeViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvInfo;
        ImageView ivEpisodeImage;
        ImageButton btnEdit, btnDelete;

        public EpisodeViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvEpisodeTitle);
            tvInfo = itemView.findViewById(R.id.tvEpisodeInfo);
            ivEpisodeImage = itemView.findViewById(R.id.ivEpisodeImage);
            btnEdit = itemView.findViewById(R.id.btnEditEpisode);
            btnDelete = itemView.findViewById(R.id.btnDeleteEpisode);
        }
    }
}
