package app.edu.app.adapter;

import android.annotation.SuppressLint;
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

public class ThucUongHomeFragmentAdapter extends RecyclerView.Adapter<ThucUongHomeFragmentAdapter.ThucUongViewHolder> {
    ArrayList<HangHoa> list;
    private ItemHangHoaOnClick itemHangHoaOnClick;

    public ThucUongHomeFragmentAdapter(ArrayList<HangHoa> list, ItemHangHoaOnClick itemHangHoaOnClick) {
        this.list = list;
        this.itemHangHoaOnClick = itemHangHoaOnClick;
    }

    @NonNull
    @Override
    public ThucUongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_item_thuc_uong_homefragment, parent, false);
        return new ThucUongViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull ThucUongViewHolder holder, int position) {
        HangHoa hangHoa = list.get(position);
        if (hangHoa == null) {
            return;
        }
        ImageCache.getUrlFromCache("" + hangHoa.getMaHangHoa(), new OnUrlFetchedListener() {
            @Override
            public void onUrlFetched(String url) {
                Picasso.get().load(url).into(holder.ivHinhAnh);
            }
        });
        holder.tvTenHangHoa.setText(hangHoa.getTenHangHoa());
        holder.tvGiaTien.setText(hangHoa.getGiaTien() + "VND");
        
        // Click listener
        holder.itemView.setOnClickListener(v -> {
            if (itemHangHoaOnClick != null) {
                itemHangHoaOnClick.itemOclick(v, hangHoa);
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
        ImageView ivHinhAnh;
        TextView tvTenHangHoa, tvGiaTien;

        public ThucUongViewHolder(@NonNull View itemView) {
            super(itemView);
            ivHinhAnh = itemView.findViewById(R.id.ivHinhAnh);
            tvTenHangHoa = itemView.findViewById(R.id.tvTenHangHoa);
            tvGiaTien = itemView.findViewById(R.id.tvGiaTien);
        }
    }
}
