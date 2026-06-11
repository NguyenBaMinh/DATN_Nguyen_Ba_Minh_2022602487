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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import app.edu.app.R;
import app.edu.app.interfaces.ItemBanOnClick;
import app.edu.app.model.Ban;

/**
 * Adapter hiển thị danh sách bàn
 * 
 * HỖ TRỢ 2 NGỮ CẢNH:
 * 
 * 1. QUẢN LÝ BÀN (QuanLyBanActivity):
 *    - Dùng Ban.trangThai để hiển thị trạng thái HIỆN TẠI (có khách đang ngồi hay không)
 *    - Ban.trangThai = 0: Trống → Có thể tạo hóa đơn
 *    - Ban.trangThai = 1: Có khách → Xem hóa đơn
 * 
 * 2. ĐẶT BÀN (DatBanActivity):
 *    - Ban: Chỉ dùng để lấy danh sách các bàn (maBan)
 *    - DatBan: Dùng để xác định bàn nào đã được đặt cho tương lai (bookedTableIds)
 *    - Bỏ qua Ban.trangThai - không dùng để xác định bàn đã đặt
 */
public class BanAdapter extends RecyclerView.Adapter<BanAdapter.BanViewHolder> {
    ArrayList<Ban> list;
    ItemBanOnClick itemBanOnClick;
    Set<Integer> bookedTableIds; // Danh sách mã bàn đã được đặt trong ngày đã chọn (từ DatBan)
    private boolean isDatBanContext; // true = màn hình đặt bàn, false = màn hình quản lý bàn
    private Integer selectedTableId; // Mã bàn đang được chọn (để highlight)

    public BanAdapter(ArrayList<Ban> list, ItemBanOnClick itemBanOnClick) {
        this(list, itemBanOnClick, false);
    }
    
    public BanAdapter(ArrayList<Ban> list, ItemBanOnClick itemBanOnClick, boolean isDatBanContext) {
        this.list = list;
        this.itemBanOnClick = itemBanOnClick;
        this.bookedTableIds = new HashSet<>();
        this.isDatBanContext = isDatBanContext;
        this.selectedTableId = null;
    }

    public void setBookedTableIds(Set<Integer> bookedTableIds) {
        this.bookedTableIds = bookedTableIds != null ? bookedTableIds : new HashSet<>();
        notifyDataSetChanged();
    }

    public void updateList(ArrayList<Ban> newList) {
        this.list = newList != null ? newList : new ArrayList<>();
        notifyDataSetChanged();
    }
    
    /**
     * Set bàn đang được chọn để highlight
     */
    public void setSelectedTableId(Integer tableId) {
        this.selectedTableId = tableId;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BanViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_item_ban, parent, false);
        return new BanViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull BanViewHolder holder, int position) {
        Ban ban = list.get(position);
        if (ban == null){
            return;
        }
        
        if (isDatBanContext) {
            // NGỮ CẢNH: ĐẶT BÀN (DatBanActivity)
            // Logic: Dựa vào DatBan để xác định bàn đã được đặt
            boolean isBooked = bookedTableIds.contains(ban.getMaBan());
            boolean isSelected = selectedTableId != null && selectedTableId == ban.getMaBan();
            
            if (isBooked) {
                // Bàn đã được đặt trong ngày đã chọn (từ DatBan) - hiển thị màu xám (disabled)
                holder.ivHinhAnh.setImageResource(R.drawable.ic_quan_ly_ban_24_black);
                holder.ivHinhAnh.setAlpha(0.5f);
                holder.cardView.setAlpha(0.5f);
                holder.cardView.setCardBackgroundColor(holder.itemView.getContext().getResources().getColor(R.color.Gray1Primary));
                holder.tvMaBan.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.Gray1Primary));
                holder.tvMaBan.setText("BO" + ban.getMaBan() + "\n(Đã đặt)");
                holder.cardView.setClickable(false);
                holder.cardView.setEnabled(false);
            } else if (isSelected) {
                // Bàn đang được chọn - hiển thị màu xanh lá (highlight)
                holder.ivHinhAnh.setImageResource(R.drawable.ic_quan_ly_ban_24_brow);
                holder.ivHinhAnh.setAlpha(1.0f);
                holder.cardView.setAlpha(1.0f);
                holder.cardView.setCardBackgroundColor(holder.itemView.getContext().getResources().getColor(R.color.GreenPrimary));
                holder.tvMaBan.setTextColor(android.graphics.Color.WHITE);
                holder.tvMaBan.setText("BO" + ban.getMaBan() + "\n✓ Đã chọn");
                holder.cardView.setClickable(true);
                holder.cardView.setEnabled(true);
                holder.cardView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (itemBanOnClick != null) {
                            itemBanOnClick.itemOclick(view, ban);
                        }
                    }
                });
            } else {
                // Bàn chưa được đặt và chưa chọn - hiển thị màu trắng (available)
                // Bỏ qua Ban.trangThai vì Ban chỉ dùng để hiển thị danh sách bàn
                holder.ivHinhAnh.setImageResource(R.drawable.ic_quan_ly_ban_24_black);
                holder.ivHinhAnh.setAlpha(1.0f);
                holder.cardView.setAlpha(1.0f);
                holder.cardView.setCardBackgroundColor(android.graphics.Color.WHITE);
                holder.tvMaBan.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.BlackPrimary));
                holder.tvMaBan.setText("BO" + ban.getMaBan());
                holder.cardView.setClickable(true);
                holder.cardView.setEnabled(true);
                holder.cardView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (itemBanOnClick != null) {
                            itemBanOnClick.itemOclick(view, ban);
                        }
                    }
                });
            }
        } else {
            // NGỮ CẢNH: QUẢN LÝ BÀN (QuanLyBanActivity)
            // Logic: Dựa vào Ban.trangThai để hiển thị trạng thái hiện tại
            if (ban.getTrangThai() == Ban.CON_TRONG) {
                // Bàn trống - có thể tạo hóa đơn
                holder.ivHinhAnh.setImageResource(R.drawable.ic_quan_ly_ban_24_black);
                holder.ivHinhAnh.setAlpha(1.0f);
                holder.cardView.setAlpha(1.0f);
                holder.cardView.setCardBackgroundColor(android.graphics.Color.WHITE);
                holder.tvMaBan.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.BlackPrimary));
                holder.tvMaBan.setText("BO" + ban.getMaBan());
                holder.cardView.setClickable(true);
                holder.cardView.setEnabled(true);
                holder.cardView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (itemBanOnClick != null) {
                            itemBanOnClick.itemOclick(view, ban);
                        }
                    }
                });
            } else {
                // Bàn có khách - xem hóa đơn
                holder.ivHinhAnh.setImageResource(R.drawable.ic_quan_ly_ban_24_brow);
                holder.ivHinhAnh.setAlpha(1.0f);
                holder.cardView.setAlpha(0.7f);
                holder.cardView.setCardBackgroundColor(holder.itemView.getContext().getResources().getColor(R.color.YellowPrimary));
                holder.tvMaBan.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.BlackPrimary));
                holder.tvMaBan.setText("BO" + ban.getMaBan() + "\n(Có khách)");
                holder.cardView.setClickable(true);
                holder.cardView.setEnabled(true);
                holder.cardView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (itemBanOnClick != null) {
                            itemBanOnClick.itemOclick(view, ban);
                        }
                    }
                });
            }
        }
    }

    @Override
    public int getItemCount() {
        if (list == null) {
            return 0;
        }
        return list.size();
    }

    public static class BanViewHolder extends RecyclerView.ViewHolder {
        ImageView ivHinhAnh;
        TextView tvMaBan;
        CardView cardView;

        public BanViewHolder(@NonNull View itemView) {
            super(itemView);
            ivHinhAnh = itemView.findViewById(R.id.ivHinhAnh);
            tvMaBan = itemView.findViewById(R.id.tvMaBan);
            cardView = itemView.findViewById(R.id.cardView);
        }
    }
}
