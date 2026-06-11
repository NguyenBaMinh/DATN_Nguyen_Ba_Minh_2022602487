package app.edu.app.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import app.edu.app.R;
import app.edu.app.interfaces.ItemThongBaoOnClick;
import app.edu.app.model.ThongBao;
import app.edu.app.utils.XDate;

public class ThongBaoAdapter extends RecyclerView.Adapter<ThongBaoAdapter.ThongBaoViewHolder> {
    Context context;
    ArrayList<ThongBao> list;
    ItemThongBaoOnClick itemThongBaoOnClick;

    public ThongBaoAdapter(Context context, ArrayList<ThongBao> list, ItemThongBaoOnClick itemThongBaoOnClick) {
        this.context = context;
        this.list = list;
        this.itemThongBaoOnClick = itemThongBaoOnClick;
    }

    @NonNull
    @Override
    public ThongBaoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_item_thong_bao, parent, false);
        return new ThongBaoViewHolder(view);
    }

    @SuppressLint({"ResourceType", "UseCompatLoadingForDrawables"})
    @Override
    public void onBindViewHolder(@NonNull ThongBaoViewHolder holder, int position) {
        ThongBao thongBao = list.get(position);
        if(thongBao == null){
            return;
        }

        // Hiển thị tiêu đề
        String tieuDe = thongBao.getTieuDe();
        holder.tvTieuDe.setText(tieuDe != null && !tieuDe.isEmpty() ? tieuDe : "Thông báo");
        
        // Hiển thị nội dung
        holder.tvNoiDung.setText(thongBao.getNoiDung());
        holder.tvNgayThongBao.setText(XDate.toStringDate(thongBao.getNgayThongBao()));

        if(thongBao.getTrangThai() == ThongBao.STATUS_CHUA_XEM){
            holder.ivHinhAnh.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_thong_bao_chua_xem));
        }else {
            holder.ivHinhAnh.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_thong_bao_da_xem));
        }

        holder.ivMenuMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                itemThongBaoOnClick.itemOclick(view, thongBao);
            }
        });
    }

    @Override
    public int getItemCount() {
        if(list == null){
            return 0;
        }
        return list.size();
    }

    public static class ThongBaoViewHolder extends RecyclerView.ViewHolder {
        TextView tvTieuDe, tvNoiDung, tvNgayThongBao;
        RelativeLayout relativeLayout;
        ImageView ivHinhAnh, ivMenuMore;
        public ThongBaoViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTieuDe = itemView.findViewById(R.id.tvTieuDe);
            tvNoiDung = itemView.findViewById(R.id.tvNoiDung);
            tvNgayThongBao = itemView.findViewById(R.id.tvNgayThongBao);
            relativeLayout = itemView.findViewById(R.id.layoutThongBao);
            ivHinhAnh = itemView.findViewById(R.id.ivHinhAnh);
            ivMenuMore = itemView.findViewById(R.id.ivMenuMore);
        }
    }
}
