package app.edu.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;
import app.edu.app.R;
import app.edu.app.interfaces.ItemNguoiDungOnClick;
import app.edu.app.interfaces.OnUrlFetchedListener;
import app.edu.app.model.NguoiDung;
import app.edu.app.utils.ImageCache;

public class NguoiDungAdapter extends RecyclerView.Adapter<NguoiDungAdapter.NguoiDungViewHodel>{
    ArrayList<NguoiDung> list;
    ItemNguoiDungOnClick itemNguoiDungOnClick;

    public NguoiDungAdapter(ArrayList<NguoiDung> list, ItemNguoiDungOnClick itemNguoiDungOnClick) {
        this.list = list;
        this.itemNguoiDungOnClick = itemNguoiDungOnClick;
    }

    @NonNull
    @Override
    public NguoiDungViewHodel onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_item_nhan_vien, parent, false);
        return new NguoiDungViewHodel(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NguoiDungViewHodel holder, int position) {
        NguoiDung nguoiDung = list.get(position);
        if (nguoiDung == null){
            return;
        }

        holder.tvTenNguoiDung.setText(nguoiDung.getHoVaTen());
        holder.tvEmail.setText(nguoiDung.getEmail());
//        Bitmap bitmap = BitmapFactory.decodeByteArray(nguoiDung.getHinhAnh(),
//                0,
//                nguoiDung.getHinhAnh().length);
//        holder.ivHinhAnh.setImageBitmap(bitmap);

        ImageCache.getUrlFromCache("nguoidung_" + nguoiDung.getMaNguoiDung(), new OnUrlFetchedListener() {
            @Override
            public void onUrlFetched(String url) {
                Picasso.get().load(url).into(holder.ivHinhAnh);
            }
        });
        holder.ivMenuMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                itemNguoiDungOnClick.itemOclick(view, nguoiDung);
            }
        });
    }

    @Override
    public int getItemCount() {
        if (list == null){
            return 0;
        }
        return list.size();
    }

    public static class NguoiDungViewHodel extends RecyclerView.ViewHolder {
        CircleImageView ivHinhAnh;
        TextView tvEmail, tvTenNguoiDung;
        ImageView ivMenuMore;

        public NguoiDungViewHodel(@NonNull View itemView) {
            super(itemView);
            ivHinhAnh = itemView.findViewById(R.id.ivHinhAnh);
            tvTenNguoiDung = itemView.findViewById(R.id.tvTenNguoiDung);
            tvEmail = itemView.findViewById(R.id.tvEmail);
            ivMenuMore = itemView.findViewById(R.id.ivMenuMore);

        }
    }

}
