package app.edu.app.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
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
import app.edu.app.interfaces.ItemTangGiamSoLuongOnClick;
import app.edu.app.interfaces.OnUrlFetchedListener;
import app.edu.app.model.HangHoa;
import app.edu.app.model.HoaDonChiTiet;
import app.edu.app.utils.ImageCache;

/**
 * Adapter hiển thị danh sách món ăn trong hóa đơn
 * ✅ ĐÃ FIX: Xóa biến index shared, dùng hoaDonChiTiet.getSoLuong()
 */
public class HoaDonChiTietMainAdapter extends RecyclerView.Adapter<HoaDonChiTietMainAdapter.HoaDonChiTietMainViewHolder> {
    Context context;
    ArrayList<HangHoa> list;
    ArrayList<HoaDonChiTiet> listHDCT;
    ItemTangGiamSoLuongOnClick itemTangGiamSoLuongOnClick;

    public HoaDonChiTietMainAdapter(Context context, ArrayList<HangHoa> list, ArrayList<HoaDonChiTiet> listHDCT, ItemTangGiamSoLuongOnClick itemTangGiamSoLuongOnClick) {
        this.context = context;
        this.list = list;
        this.listHDCT = listHDCT;
        this.itemTangGiamSoLuongOnClick = itemTangGiamSoLuongOnClick;
        
        Log.d("Adapter", "╔══════════════════════════════╗");
        Log.d("Adapter", "║   ADAPTER CONSTRUCTOR        ║");
        Log.d("Adapter", "╚══════════════════════════════╝");
        Log.d("Adapter", "HangHoa list size: " + (list != null ? list.size() : "NULL"));
        Log.d("Adapter", "HoaDonChiTiet list size: " + (listHDCT != null ? listHDCT.size() : "NULL"));
        
        if (list != null && listHDCT != null) {
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i) != null) {
                    Log.d("Adapter", "  [" + i + "] HangHoa: " + list.get(i).getTenHangHoa() + 
                          " | HDCT: " + (listHDCT.get(i) != null ? "SL=" + listHDCT.get(i).getSoLuong() : "NULL"));
                } else {
                    Log.e("Adapter", "  [" + i + "] HangHoa is NULL!");
                }
            }
        }
    }

    @NonNull
    @Override
    public HoaDonChiTietMainViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Log.d("Adapter", "onCreateViewHolder called (viewType=" + viewType + ")");
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_item_thuc_uong_oder_main, parent, false);
        return new HoaDonChiTietMainViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull HoaDonChiTietMainViewHolder holder, int position) {
        // ✅ Validation: Kiểm tra bounds của cả 2 danh sách
        if (position >= list.size() || position >= listHDCT.size()) {
            Log.e("Adapter", "Position out of bounds: " + position);
            return;
        }
        
        HangHoa hangHoa = list.get(position);
        HoaDonChiTiet hoaDonChiTiet = listHDCT.get(position);
        
        // ✅ Validation: Kiểm tra null
        if (hangHoa == null || hoaDonChiTiet == null) {
            Log.e("Adapter", "Null object at position " + position + 
                  " - HangHoa: " + (hangHoa == null ? "NULL" : "OK") + 
                  ", HoaDonChiTiet: " + (hoaDonChiTiet == null ? "NULL" : "OK"));
            return;
        }
        
        Log.d("Adapter", "Bind position " + position + ": " + hangHoa.getTenHangHoa() + 
              ", SL=" + hoaDonChiTiet.getSoLuong() + ", MaHH=" + hangHoa.getMaHangHoa());
        
        // Load hình ảnh từ Firebase Storage
        ImageCache.getUrlFromCache("" + hangHoa.getMaHangHoa(), new OnUrlFetchedListener() {
            @Override
            public void onUrlFetched(String url) {
                Picasso.get().load(url).into(holder.ivHinhAnh);
            }
        });
        
        // Hiển thị thông tin sản phẩm
        holder.tvTenHangHoa.setText(hangHoa.getTenHangHoa());
        holder.tvGiaTienBanDau.setText(hangHoa.getGiaTien() + "VND");
        holder.tvMaHangHoa.setText("Mã sản phẩm: " + hangHoa.getMaHangHoa());
        
        // ✅ FIX: Hiển thị số lượng và giá tiền HIỆN TẠI từ HoaDonChiTiet (không dùng biến index shared)
        int currentQuantity = hoaDonChiTiet.getSoLuong();
        holder.tvSoluong.setText(String.valueOf(currentQuantity));
        holder.tvGiaTien.setText((hangHoa.getGiaTien() * currentQuantity) + "VND");
        
        // ✅ FIX: Xử lý nút TĂNG số lượng - Lấy object mới từ list mỗi lần click
        holder.ivTang.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int adapterPosition = holder.getAdapterPosition();
                Log.d("Adapter", "TĂNG clicked - position: " + adapterPosition);
                
                if (adapterPosition == RecyclerView.NO_POSITION || adapterPosition >= listHDCT.size()) {
                    Log.e("Adapter", "Invalid position: " + adapterPosition);
                    return;
                }
                
                // Lấy object MỚI NHẤT từ list
                HoaDonChiTiet currentHDCT = listHDCT.get(adapterPosition);
                HangHoa currentHH = list.get(adapterPosition);
                
                int oldQuantity = currentHDCT.getSoLuong();
                int newQuantity = oldQuantity + 1;
                if (newQuantity > 10) {
                    newQuantity = 10; // Giới hạn tối đa 10
                }
                
                Log.d("Adapter", "Item: " + currentHH.getTenHangHoa() + ", Số lượng: " + oldQuantity + " → " + newQuantity);
                
                // Cập nhật UI ngay lập tức
                holder.tvSoluong.setText(String.valueOf(newQuantity));
                holder.tvGiaTien.setText((currentHH.getGiaTien() * newQuantity) + "VND");
                
                // Callback để update vào Firebase
                itemTangGiamSoLuongOnClick.itemOclick(view, newQuantity, currentHDCT, currentHH);
            }
        });
        
        // ✅ FIX: Xử lý nút GIẢM số lượng - Lấy object mới từ list mỗi lần click
        holder.ivGiam.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int adapterPosition = holder.getAdapterPosition();
                Log.d("Adapter", "GIẢM clicked - position: " + adapterPosition);
                
                if (adapterPosition == RecyclerView.NO_POSITION || adapterPosition >= listHDCT.size()) {
                    Log.e("Adapter", "Invalid position: " + adapterPosition);
                    return;
                }
                
                // Lấy object MỚI NHẤT từ list
                HoaDonChiTiet currentHDCT = listHDCT.get(adapterPosition);
                HangHoa currentHH = list.get(adapterPosition);
                
                int oldQuantity = currentHDCT.getSoLuong();
                int newQuantity = oldQuantity - 1;
                if (newQuantity < 1) {
                    newQuantity = 1; // Giới hạn tối thiểu 1
                }
                
                Log.d("Adapter", "Item: " + currentHH.getTenHangHoa() + ", Số lượng: " + oldQuantity + " → " + newQuantity);
                
                // Cập nhật UI ngay lập tức
                holder.tvSoluong.setText(String.valueOf(newQuantity));
                holder.tvGiaTien.setText((currentHH.getGiaTien() * newQuantity) + "VND");
                
                // Callback để update vào Firebase
                itemTangGiamSoLuongOnClick.itemOclick(view, newQuantity, currentHDCT, currentHH);
            }
        });
        
        // Xử lý long click để xóa món
        holder.cardView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                itemTangGiamSoLuongOnClick.itemOclickDeleteHDCT(view, hoaDonChiTiet);
                return true;
            }
        });
    }

    @Override
    public int getItemCount() {
        // ✅ Trả về size nhỏ hơn giữa 2 danh sách để tránh IndexOutOfBounds
        if (list == null || listHDCT == null) {
            Log.w("Adapter", "getItemCount: list hoặc listHDCT là NULL, return 0");
            return 0;
        }
        int count = Math.min(list.size(), listHDCT.size());
        Log.d("Adapter", "getItemCount() = " + count + " (list.size=" + list.size() + ", listHDCT.size=" + listHDCT.size() + ")");
        return count;
    }
    
    /**
     * ✅ Update data của adapter (dùng cho real-time updates)
     * Thay vì tạo adapter mới, update data và notify để refresh UI
     * 
     * @param newListHangHoa Danh sách HangHoa mới
     * @param newListHDCT Danh sách HoaDonChiTiet mới
     */
    public void updateData(ArrayList<HangHoa> newListHangHoa, ArrayList<HoaDonChiTiet> newListHDCT) {
        Log.d("Adapter", "╔══════════════════════════════╗");
        Log.d("Adapter", "║   UPDATE DATA                 ║");
        Log.d("Adapter", "╚══════════════════════════════╝");
        Log.d("Adapter", "Old size: " + (list != null ? list.size() : 0) + " → New size: " + newListHangHoa.size());
        
        // ✅ Validation
        if (newListHangHoa == null || newListHDCT == null) {
            Log.e("Adapter", "❌ newListHangHoa hoặc newListHDCT là NULL!");
            return;
        }
        
        if (newListHangHoa.size() != newListHDCT.size()) {
            Log.e("Adapter", "❌ Size không khớp: HangHoa=" + newListHangHoa.size() + ", HDCT=" + newListHDCT.size());
            return;
        }
        
        // Clear và update lists
        if (this.list == null) {
            this.list = new ArrayList<>();
        }
        if (this.listHDCT == null) {
            this.listHDCT = new ArrayList<>();
        }
        
        // ✅ Lưu old size để notify đúng
        int oldSize = this.list.size();
        
        // Update với data mới
        this.list.clear();
        this.list.addAll(newListHangHoa);
        
        this.listHDCT.clear();
        this.listHDCT.addAll(newListHDCT);
        
        Log.d("Adapter", "✓ Data đã được update:");
        for (int i = 0; i < this.list.size(); i++) {
            Log.d("Adapter", "  [" + i + "] " + this.list.get(i).getTenHangHoa() + 
                  " - SL: " + this.listHDCT.get(i).getSoLuong());
        }
        
        // ✅ Log change để debug
        if (oldSize != this.list.size()) {
            Log.d("Adapter", "📊 Size changed: " + oldSize + " → " + this.list.size());
        }
    }

    public static class HoaDonChiTietMainViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        ImageView ivHinhAnh, ivGiam, ivTang;
        TextView tvTenHangHoa, tvSoluong, tvGiaTien, tvGiaTienBanDau, tvMaHangHoa;

        public HoaDonChiTietMainViewHolder(@NonNull View itemView) {
            super(itemView);
            ivHinhAnh = itemView.findViewById(R.id.ivHinhAnh);
            tvTenHangHoa = itemView.findViewById(R.id.tvTenHangHoa);
            tvSoluong = itemView.findViewById(R.id.tvSoluong);
            tvGiaTien = itemView.findViewById(R.id.tvGiaTien);
            tvGiaTienBanDau = itemView.findViewById(R.id.tvGiaTienBanDau);
            ivGiam = itemView.findViewById(R.id.ivGiamSoLuong);
            ivTang = itemView.findViewById(R.id.ivTangSoLuong);
            cardView = itemView.findViewById(R.id.cardView);
            tvMaHangHoa = itemView.findViewById(R.id.tvMaSanPham);
        }
    }
}
