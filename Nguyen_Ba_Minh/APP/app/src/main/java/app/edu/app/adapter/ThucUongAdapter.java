package app.edu.app.adapter;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import app.edu.app.R;
import app.edu.app.interfaces.ItemHangHoaOnClick;
import app.edu.app.interfaces.OnUrlFetchedListener;
import app.edu.app.model.HangHoa;
import app.edu.app.utils.ImageCache;

public class ThucUongAdapter extends RecyclerView.Adapter<ThucUongAdapter.ThucUongViewHolder> {
    ArrayList<HangHoa> list;
    ItemHangHoaOnClick itemHangHoaOnClick;

    public ThucUongAdapter(ArrayList<HangHoa> list, ItemHangHoaOnClick itemHangHoaOnClick) {
        this.list = list;
        this.itemHangHoaOnClick = itemHangHoaOnClick;
    }

    @NonNull
    @Override
    public ThucUongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_item_thuc_uong, parent, false);
        return new ThucUongViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull ThucUongViewHolder holder, int position) {
        HangHoa hangHoa = list.get(position);
        if (hangHoa == null) {
            return;
        }

        // Set placeholder image first
        holder.ivHinhAnh.setImageResource(R.drawable.sample_data_hanghoa_cfmay);
        
        // Try to load image from Firebase Storage
        ImageCache.getUrlFromCache("" + hangHoa.getMaHangHoa(), new OnUrlFetchedListener() {
            @Override
            public void onUrlFetched(String url) {
                if (url != null && !url.isEmpty()) {
                    Picasso.get()
                            .load(url)
                            .placeholder(R.drawable.sample_data_hanghoa_cfmay)
                            .error(R.drawable.sample_data_hanghoa_cfmay)
                            .into(holder.ivHinhAnh);
                } else {
                    // If URL is null, keep the placeholder
                    holder.ivHinhAnh.setImageResource(R.drawable.sample_data_hanghoa_cfmay);
                }
            }
        });
        holder.tvTenHangHoa.setText(hangHoa.getTenHangHoa());
        holder.tvGiaTien.setText(hangHoa.getGiaTien() + "VND");
        holder.tvMaHangHoa.setText("Mã sản phẩm: " + String.valueOf(hangHoa.getMaHangHoa()));
        if (hangHoa.getTrangThai() == 0) {
            holder.tvTrangThai.setText("Hết hàng");
            holder.tvTrangThai.setTextColor(Color.GRAY);
        } else {
            holder.tvTrangThai.setText("Còn hàng");
            holder.tvTrangThai.setTextColor(Color.BLUE);
        }
        holder.ivMenuMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                itemHangHoaOnClick.itemOclick(view, hangHoa);
            }
        });

    }

    @Override
    public int getItemCount() {
        if (list == null) {
            return 0;
        }
        return list.size();
    }

    public static class ThucUongViewHolder extends RecyclerView.ViewHolder {
        ImageView ivHinhAnh, ivMenuMore;
        TextView tvTenHangHoa, tvGiaTien, tvTrangThai, tvMaHangHoa;

        public ThucUongViewHolder(@NonNull View itemView) {
            super(itemView);
            ivHinhAnh = itemView.findViewById(R.id.ivHinhAnh);
            ivMenuMore = itemView.findViewById(R.id.ivMenuMore);
            tvTenHangHoa = itemView.findViewById(R.id.tvTenHangHoa);
            tvGiaTien = itemView.findViewById(R.id.tvGiaTien);
            tvTrangThai = itemView.findViewById(R.id.tvTrangThai);
            tvMaHangHoa = itemView.findViewById(R.id.tvMaSanPham);
        }
    }
}
