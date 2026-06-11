package app.edu.app.adapter;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import app.edu.app.R;
import app.edu.app.interfaces.ItemDatBanOnClick;
import app.edu.app.model.DatBan;

public class DatBanAdapter extends RecyclerView.Adapter<DatBanAdapter.DatBanViewHolder> {
    ArrayList<DatBan> list;
    ItemDatBanOnClick itemDatBanOnClick;
    SimpleDateFormat inputFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault());
    SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

    public DatBanAdapter(ArrayList<DatBan> list, ItemDatBanOnClick itemDatBanOnClick) {
        this.list = list;
        this.itemDatBanOnClick = itemDatBanOnClick;
    }

    @NonNull
    @Override
    public DatBanViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_item_dat_ban, parent, false);
        return new DatBanViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull DatBanViewHolder holder, int position) {
        DatBan datBan = list.get(position);
        if (datBan == null) {
            return;
        }

        // Hiển thị tên bàn (ưu tiên tenBan từ Firebase, nếu không có thì dùng maBan)
        if (datBan.getTenBan() != null && !datBan.getTenBan().isEmpty()) {
            holder.tvMaBan.setText(datBan.getTenBan());
        } else {
            holder.tvMaBan.setText("Bàn " + datBan.getMaBan());
        }

        // Hiển thị ngày giờ sử dụng
        try {
            Date ngayGioSuDung = inputFormat.parse(datBan.getNgayGioSuDung());
            if (ngayGioSuDung != null) {
                holder.tvNgay.setText("Ngày: " + dateFormat.format(ngayGioSuDung));
                holder.tvGio.setText("Giờ: " + timeFormat.format(ngayGioSuDung));
            } else {
                holder.tvNgay.setText("Ngày: " + datBan.getNgayGioSuDung());
                holder.tvGio.setText("");
            }
        } catch (ParseException e) {
            holder.tvNgay.setText("Ngày: " + datBan.getNgayGioSuDung());
            holder.tvGio.setText("");
        }

        // Hiển thị trạng thái
        String trangThaiText;
        int iconResource;
        int backgroundResource;
        int textColor;
        switch (datBan.getTrangThai()) {
            case DatBan.TRANG_THAI_DA_DUYET:
                trangThaiText = "Đã duyệt";
                iconResource = R.drawable.ic_quan_ly_ban_24_brow;
                backgroundResource = R.drawable.bg_status_approved;
                textColor = android.graphics.Color.WHITE;
                break;
            case DatBan.TRANG_THAI_CHO_DUYET:
                trangThaiText = "Chờ duyệt";
                iconResource = R.drawable.ic_quan_ly_ban_24_black;
                backgroundResource = R.drawable.bg_status_pending;
                textColor = android.graphics.Color.BLACK;
                break;
            case DatBan.TRANG_THAI_TU_CHOI:
                trangThaiText = "Từ chối";
                iconResource = R.drawable.ic_quan_ly_ban_24_black;
                backgroundResource = R.drawable.bg_status_rejected;
                textColor = android.graphics.Color.WHITE;
                break;
            default:
                trangThaiText = "Không xác định";
                iconResource = R.drawable.ic_quan_ly_ban_24_black;
                backgroundResource = R.drawable.custom_button;
                textColor = android.graphics.Color.WHITE;
                break;
        }
        holder.tvTrangThai.setText(trangThaiText);
        holder.tvTrangThai.setBackgroundResource(backgroundResource);
        holder.tvTrangThai.setTextColor(textColor);
        holder.ivIcon.setImageResource(iconResource);

        // Ghi chú
        if (datBan.getGhiChu() != null && !datBan.getGhiChu().isEmpty()) {
            holder.tvGhiChu.setVisibility(View.VISIBLE);
            holder.tvGhiChu.setText("Ghi chú: " + datBan.getGhiChu());
        } else {
            holder.tvGhiChu.setVisibility(View.GONE);
        }

        holder.cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (itemDatBanOnClick != null) {
                    itemDatBanOnClick.itemOclick(view, datBan);
                }
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

    public static class DatBanViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvMaBan, tvNgay, tvGio, tvTrangThai, tvGhiChu;
        CardView cardView;

        public DatBanViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.ivIcon);
            tvMaBan = itemView.findViewById(R.id.tvMaBan);
            tvNgay = itemView.findViewById(R.id.tvNgay);
            tvGio = itemView.findViewById(R.id.tvGio);
            tvTrangThai = itemView.findViewById(R.id.tvTrangThai);
            tvGhiChu = itemView.findViewById(R.id.tvGhiChu);
            cardView = itemView.findViewById(R.id.cardView);
        }
    }
}

