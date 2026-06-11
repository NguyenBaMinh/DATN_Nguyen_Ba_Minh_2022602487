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
import app.edu.app.interfaces.ItemOderOnClick;
import app.edu.app.interfaces.OnUrlFetchedListener;
import app.edu.app.model.HangHoa;
import app.edu.app.utils.ImageCache;

public class ThucUongOderThemAdapter extends RecyclerView.Adapter<ThucUongOderThemAdapter.ThucUongViewHolder> {
    ArrayList<HangHoa> list;
    ItemOderOnClick itemOderOnClick;

    public ThucUongOderThemAdapter(ArrayList<HangHoa> list , ItemOderOnClick itemOderOnClick){
        this.list = list;
        this.itemOderOnClick = itemOderOnClick;
    }

    @NonNull
    @Override
    public ThucUongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_item_thuc_uong_oder, parent, false);
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
        holder.tvMaHangHoa.setText("Mã sản phẩm: "+String.valueOf(hangHoa.getMaHangHoa()));
        holder.ivThem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                itemOderOnClick.itemOclick(view, hangHoa);
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
        ImageView ivHinhAnh, ivThem;
        TextView tvTenHangHoa, tvGiaTien, tvMaHangHoa;

        public ThucUongViewHolder(@NonNull View itemView) {
            super(itemView);
            ivHinhAnh = itemView.findViewById(R.id.ivHinhAnh);
            ivThem = itemView.findViewById(R.id.ivThem);
            tvTenHangHoa = itemView.findViewById(R.id.tvTenHangHoa);
            tvGiaTien = itemView.findViewById(R.id.tvGiaTien);
            tvMaHangHoa = itemView.findViewById(R.id.tvMaSanPham);
        }
    }
}
