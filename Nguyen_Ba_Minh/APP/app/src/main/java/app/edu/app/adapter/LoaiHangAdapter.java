package app.edu.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import app.edu.app.R;
import app.edu.app.interfaces.ItemLoaiHangOnClick;
import app.edu.app.interfaces.OnUrlFetchedListener;
import app.edu.app.model.LoaiHang;
import app.edu.app.utils.ImageCache;

public class LoaiHangAdapter extends RecyclerView.Adapter<LoaiHangAdapter.LoaiHangViewHolder> {

    private ArrayList<LoaiHang> list;
    private ItemLoaiHangOnClick itemOnClick;
    private int selectedPosition = -1; // Track selected position

    public LoaiHangAdapter(ArrayList<LoaiHang> list, ItemLoaiHangOnClick itemOnClick) {
        this.list = list;
        this.itemOnClick = itemOnClick;
    }

    @NonNull
    @Override
    public LoaiHangViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_item_loai_hang, parent, false);
        return new LoaiHangViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LoaiHangViewHolder holder, int position) {
        LoaiHang loaiHang = list.get(position);

        if (loaiHang == null) {
            return;
        }

        // Set name
        holder.tvTenLoaiHang.setText(loaiHang.getTenLoai());
        
        // Set maLoai (hidden but accessible)
        holder.tvMaLoaiHang.setText(String.valueOf(loaiHang.getMaLoai()));

        // Load image
        if (loaiHang.getMaLoai() == -1) {
            // "Tất cả" item - show gradient background with icon
            holder.ivHinhAnh.setImageResource(R.drawable.ic_all_categories);
            holder.ivHinhAnh.setPadding(20, 20, 20, 20);
            holder.ivHinhAnh.setScaleType(ImageView.ScaleType.FIT_CENTER);
            holder.ivHinhAnh.setBackgroundResource(R.drawable.bg_all_categories);
        } else {
            // Load from Firebase Storage
            holder.ivHinhAnh.setPadding(0, 0, 0, 0);
            holder.ivHinhAnh.setScaleType(ImageView.ScaleType.CENTER_CROP);
            holder.ivHinhAnh.setBackgroundResource(0); // Clear background
            
            ImageCache.getUrlFromCache("loaihang_" + loaiHang.getMaLoai(), new OnUrlFetchedListener() {
                @Override
                public void onUrlFetched(String url) {
                    Picasso.get()
                            .load(url)
                            .placeholder(R.drawable.slide_image1)
                            .error(R.drawable.slide_image1)
                            .into(holder.ivHinhAnh);
                }
            });
        }

        // Handle selected state
        boolean isSelected = (position == selectedPosition);
        updateSelectedState(holder, isSelected);

        // Click listener
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Update selected position
                int previousPosition = selectedPosition;
                selectedPosition = holder.getAdapterPosition();
                
                // Notify changes for animations
                if (previousPosition != -1) {
                    notifyItemChanged(previousPosition);
                }
                notifyItemChanged(selectedPosition);
                
                // Callback
                if (itemOnClick != null) {
                    itemOnClick.itemOclick(view, loaiHang);
                }
            }
        });
    }

    /**
     * Update UI for selected state
     */
    private void updateSelectedState(LoaiHangViewHolder holder, boolean isSelected) {
        if (isSelected) {
            // Selected state - Highlight with overlay and checkmark
            holder.cardImageContainer.setCardElevation(12f);
            holder.cardImageContainer.setRadius(16f);
            holder.viewOverlay.setVisibility(View.VISIBLE);
            holder.ivCheckMark.setVisibility(View.VISIBLE);
            holder.tvTenLoaiHang.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.BrowPrimary));
            holder.tvTenLoaiHang.setTypeface(null, android.graphics.Typeface.BOLD);
            
            // Scale animation
            holder.itemView.setScaleX(1.05f);
            holder.itemView.setScaleY(1.05f);
        } else {
            // Normal state
            holder.cardImageContainer.setCardElevation(4f);
            holder.cardImageContainer.setRadius(16f);
            holder.viewOverlay.setVisibility(View.GONE);
            holder.ivCheckMark.setVisibility(View.GONE);
            holder.tvTenLoaiHang.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.BlackPrimary));
            holder.tvTenLoaiHang.setTypeface(null, android.graphics.Typeface.NORMAL);
            
            // Reset scale
            holder.itemView.setScaleX(1.0f);
            holder.itemView.setScaleY(1.0f);
        }
        
        // Animate transition
        holder.itemView.animate()
                .scaleX(isSelected ? 1.05f : 1.0f)
                .scaleY(isSelected ? 1.05f : 1.0f)
                .setDuration(200)
                .start();
    }

    /**
     * Reset selection (show all)
     */
    public void resetSelection() {
        int previousPosition = selectedPosition;
        selectedPosition = -1;
        if (previousPosition != -1) {
            notifyItemChanged(previousPosition);
        }
    }

    @Override
    public int getItemCount() {
        if (list == null) {
            return 0;
        }
        return list.size();
    }

    public static class LoaiHangViewHolder extends RecyclerView.ViewHolder {
        CardView cardImageContainer;
        ImageView ivHinhAnh;
        ImageView ivCheckMark;
        View viewOverlay;
        TextView tvTenLoaiHang, tvMaLoaiHang;

        public LoaiHangViewHolder(@NonNull View itemView) {
            super(itemView);
            cardImageContainer = itemView.findViewById(R.id.cardImageContainer);
            ivHinhAnh = itemView.findViewById(R.id.ivHinhAnh);
            ivCheckMark = itemView.findViewById(R.id.ivCheckMark);
            viewOverlay = itemView.findViewById(R.id.viewOverlay);
            tvTenLoaiHang = itemView.findViewById(R.id.tvTenLoaiHang);
            tvMaLoaiHang = itemView.findViewById(R.id.tvMaLoaiHang);
        }
    }
}

