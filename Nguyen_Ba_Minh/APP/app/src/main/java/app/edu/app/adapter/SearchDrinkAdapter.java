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

/**
 * Adapter cho hiển thị thức uống trong trang tìm kiếm (grid 2 cột)
 */
public class SearchDrinkAdapter extends RecyclerView.Adapter<SearchDrinkAdapter.SearchDrinkViewHolder> {
    private ArrayList<HangHoa> list;
    private ItemHangHoaOnClick itemHangHoaOnClick;

    public SearchDrinkAdapter(ArrayList<HangHoa> list, ItemHangHoaOnClick itemHangHoaOnClick) {
        this.list = list;
        this.itemHangHoaOnClick = itemHangHoaOnClick;
    }

    @NonNull
    @Override
    public SearchDrinkViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_item_search_drink, parent, false);
        return new SearchDrinkViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull SearchDrinkViewHolder holder, int position) {
        HangHoa hangHoa = list.get(position);
        if (hangHoa == null) {
            return;
        }

        // Load hình ảnh từ cache/Firebase
        ImageCache.getUrlFromCache("" + hangHoa.getMaHangHoa(), new OnUrlFetchedListener() {
            @Override
            public void onUrlFetched(String url) {
                if (url != null && !url.isEmpty()) {
                    Picasso.get().load(url).into(holder.ivHinhAnh);
                }
            }
        });

        // Set tên và giá
        holder.tvTenHangHoa.setText(hangHoa.getTenHangHoa());
        holder.tvGiaTien.setText(formatPrice(hangHoa.getGiaTien()));

        // Click listener
        holder.itemView.setOnClickListener(v -> {
            if (itemHangHoaOnClick != null) {
                itemHangHoaOnClick.itemOclick(v, hangHoa);
            }
        });
    }

    @Override
    public int getItemCount() {
        return list != null ? list.size() : 0;
    }

    /**
     * Format giá tiền
     */
    private String formatPrice(int price) {
        return String.format("%,dđ", price).replace(",", ".");
    }

    public static class SearchDrinkViewHolder extends RecyclerView.ViewHolder {
        ImageView ivHinhAnh;
        TextView tvTenHangHoa;
        TextView tvGiaTien;

        public SearchDrinkViewHolder(@NonNull View itemView) {
            super(itemView);
            ivHinhAnh = itemView.findViewById(R.id.ivHinhAnh);
            tvTenHangHoa = itemView.findViewById(R.id.tvTenHangHoa);
            tvGiaTien = itemView.findViewById(R.id.tvGiaTien);
        }
    }
}

