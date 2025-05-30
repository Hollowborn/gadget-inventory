package com.example.gadgetinventory.ui.inventory;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.gadgetinventory.R;
import com.example.gadgetinventory.data.entity.GadgetEntity;
import com.google.android.material.chip.Chip;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class GadgetAdapter extends ListAdapter<GadgetEntity, GadgetAdapter.GadgetViewHolder> {
    private final OnGadgetClickListener listener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());

    public interface OnGadgetClickListener {
        void onGadgetClick(GadgetEntity gadget);
    }

    public GadgetAdapter(OnGadgetClickListener listener) {
        super(new DiffUtil.ItemCallback<GadgetEntity>() {
            @Override
            public boolean areItemsTheSame(@NonNull GadgetEntity oldItem, @NonNull GadgetEntity newItem) {
                return oldItem.getId() == newItem.getId();
            }

            @Override
            public boolean areContentsTheSame(@NonNull GadgetEntity oldItem, @NonNull GadgetEntity newItem) {
                return oldItem.getName().equals(newItem.getName()) &&
                        oldItem.getModel().equals(newItem.getModel()) &&
                        oldItem.getCondition().equals(newItem.getCondition()) &&
                        oldItem.getPurchaseDate().equals(newItem.getPurchaseDate()) &&
                        oldItem.getEstimatedValue() == newItem.getEstimatedValue() &&
                        oldItem.getImageUri().equals(newItem.getImageUri());
            }
        });
        this.listener = listener;
    }

    @NonNull
    @Override
    public GadgetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_gadget, parent, false);
        return new GadgetViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GadgetViewHolder holder, int position) {
        GadgetEntity gadget = getItem(position);
        holder.bind(gadget, listener);
    }

    static class GadgetViewHolder extends RecyclerView.ViewHolder {
        private final ImageView gadgetImage;
        private final TextView gadgetName;
        private final TextView gadgetModel;
        private final Chip gadgetCondition;
        private final TextView purchaseDate;
        private final TextView estimatedValue;
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());

        public GadgetViewHolder(@NonNull View itemView) {
            super(itemView);
            gadgetImage = itemView.findViewById(R.id.gadgetImage);
            gadgetName = itemView.findViewById(R.id.gadgetName);
            gadgetModel = itemView.findViewById(R.id.gadgetModel);
            gadgetCondition = itemView.findViewById(R.id.gadgetCondition);
            purchaseDate = itemView.findViewById(R.id.purchaseDate);
            estimatedValue = itemView.findViewById(R.id.estimatedValue);
        }

        public void bind(GadgetEntity gadget, OnGadgetClickListener listener) {
            // Set text fields
            gadgetName.setText(gadget.getName());
            gadgetModel.setText(gadget.getModel());
            gadgetCondition.setText(gadget.getCondition());
            purchaseDate.setText(dateFormat.format(gadget.getPurchaseDate()));
            estimatedValue.setText(String.format(Locale.getDefault(), "₱%.2f",
                    gadget.getEstimatedValue()));

            // Load image using Glide
            if (gadget.getImageUri() != null && !gadget.getImageUri().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(gadget.getImageUri())
                        .placeholder(R.drawable.ic_gadget_placeholder)
                        .error(R.drawable.ic_gadget_placeholder)
                        .centerCrop()
                        .into(gadgetImage);
            } else {
                gadgetImage.setImageResource(R.drawable.ic_gadget_placeholder);
            }

            // Set condition chip color based on condition
            switch (gadget.getCondition().toLowerCase()) {
                case "good":
                    gadgetCondition.setChipBackgroundColorResource(R.color.good);
                    break;
                case "fair":
                    gadgetCondition.setChipBackgroundColorResource(R.color.fair);
                    break;
                case "poor":
                    gadgetCondition.setChipBackgroundColorResource(R.color.bad);
                    break;
            }

            // Set click listener
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onGadgetClick(gadget);
                }
            });
        }
    }
}