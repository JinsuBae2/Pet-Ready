package com.example.pet.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.pet.R;
import com.example.pet.model.RecommendedAnimal;

import java.util.ArrayList;
import java.util.List;

public class RecommendedAnimalAdapter
        extends RecyclerView.Adapter<RecommendedAnimalAdapter.AnimalViewHolder> {
    private final List<RecommendedAnimal> animals = new ArrayList<>();

    public void submitList(List<RecommendedAnimal> newAnimals) {
        animals.clear();
        if (newAnimals != null) {
            animals.addAll(newAnimals.subList(0, Math.min(5, newAnimals.size())));
        }
        notifyDataSetChanged();
    }

    public boolean isEmpty() {
        return animals.isEmpty();
    }

    @NonNull
    @Override
    public AnimalViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recommended_animal, parent, false);
        return new AnimalViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AnimalViewHolder holder, int position) {
        holder.bind(animals.get(position));
    }

    @Override
    public int getItemCount() {
        return animals.size();
    }

    static class AnimalViewHolder extends RecyclerView.ViewHolder {
        private final ImageView image;
        private final TextView breed;
        private final TextView meta;
        private final TextView detail;

        AnimalViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.ivRecommendedAnimal);
            breed = itemView.findViewById(R.id.tvRecommendedAnimalBreed);
            meta = itemView.findViewById(R.id.tvRecommendedAnimalMeta);
            detail = itemView.findViewById(R.id.tvRecommendedAnimalDetail);
        }

        void bind(RecommendedAnimal animal) {
            breed.setText(safe(animal.breedType));
            meta.setText(safe(animal.age) + " · " + safe(animal.region));
            detail.setText(safe(animal.careNm) + "\n\n" + safe(animal.matchReason));
            Glide.with(image)
                    .load(animal.filename)
                    .centerCrop()
                    .placeholder(R.drawable.bg_animal_photo_placeholder)
                    .error(R.drawable.bg_animal_photo_placeholder)
                    .into(image);
        }

        private String safe(String value) {
            return value == null ? "" : value;
        }
    }
}
