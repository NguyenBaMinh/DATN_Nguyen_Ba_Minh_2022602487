package app.edu.app.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import app.edu.app.R;
import app.edu.app.dao.HoaDonChiTietDAO;
import app.edu.app.interfaces.ItemHoaDonMangVeOnClick;
import app.edu.app.model.HoaDonMangVe;
import app.edu.app.utils.XDate;

public class DuyetAdapter extends RecyclerView.Adapter<DuyetAdapter.ViewHolder>{

    private List<HoaDonMangVe> listHoaDonMangVe;
    private Context context;
    HoaDonChiTietDAO hoaDonChiTietDAO;
    ItemHoaDonMangVeOnClick itemHoaDonOnClick;
    public DuyetAdapter(List<HoaDonMangVe> listHoaDonMangVe, Context context, ItemHoaDonMangVeOnClick itemHoaDonOnClick) {
        this.listHoaDonMangVe = listHoaDonMangVe;
        this.context = context;
        this.hoaDonChiTietDAO = new HoaDonChiTietDAO(context);
        this.itemHoaDonOnClick = itemHoaDonOnClick;

    }

    @NonNull
    @Override
    public DuyetAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.layout_item_duyet_hoa_don, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DuyetAdapter.ViewHolder holder, int position) {
        HoaDonMangVe hoaDon = listHoaDonMangVe.get(position);
        if(hoaDon == null){
            return;
        }
        holder.tvMaHoaDon.setText("CTHD"+hoaDon.getMaHoaDon());
        holder.tvtitlGioVao.setText(XDate.toStringDateTime(hoaDon.getGioVao()));
        holder.tvGioVao.setText(XDate.toStringDateTime(hoaDon.getGioVao()));
        holder.tvGioRa.setText(XDate.toStringDateTime(hoaDon.getGioRa()));
        holder.tvGiaTien.setText(hoaDonChiTietDAO.getGiaTien(hoaDon.getMaHoaDon())+"VND");
        holder.tvChiTiet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                itemHoaDonOnClick.itemOclick(view, hoaDon);
            }
        });
        holder.btnDuyet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                itemHoaDonOnClick.itemDuyet(view, hoaDon);
            }
        });
        holder.btnHuy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                itemHoaDonOnClick.itemHuy(view, hoaDon);
            }
        });
    }

    @Override
    public int getItemCount() {
        return listHoaDonMangVe.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvMaHoaDon, tvtitlGioVao, tvGioVao, tvGioRa, tvGiaTien, tvChiTiet;
        Button btnDuyet, btnHuy;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMaHoaDon = itemView.findViewById(R.id.tvMaHoaDon);
            tvtitlGioVao = itemView.findViewById(R.id.titleGioVao);
            tvGioVao = itemView.findViewById(R.id.tvGioVao);
            tvGioRa = itemView.findViewById(R.id.tvGioRa);
            tvGiaTien = itemView.findViewById(R.id.tvGiaTien);
            tvChiTiet = itemView.findViewById(R.id.tvChiTiet);
            btnDuyet = itemView.findViewById(R.id.btnDuyet);
            btnHuy = itemView.findViewById(R.id.btnHuy);
        }
    }
}
