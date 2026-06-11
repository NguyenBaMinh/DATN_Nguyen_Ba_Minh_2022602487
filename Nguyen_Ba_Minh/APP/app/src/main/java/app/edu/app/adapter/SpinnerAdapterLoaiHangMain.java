package app.edu.app.adapter;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import app.edu.app.R;
import app.edu.app.model.LoaiHang;

public class SpinnerAdapterLoaiHangMain extends BaseAdapter {
    Context context;
    ArrayList<LoaiHang> list;

    public SpinnerAdapterLoaiHangMain(Context context, ArrayList<LoaiHang> list) {
        this.context = context;
        this.list = list;
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int i) {
        return list.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        LayoutInflater layoutInflater = ((Activity) context).getLayoutInflater();
        ViewOfItem viewOfItem = null;
        if (view == null){
            view = layoutInflater.inflate(R.layout.layout_item_loai_hang_spiner_main, null);
            viewOfItem = new ViewOfItem();
            viewOfItem.tvTenLoai = view.findViewById(R.id.tvTenLoai);
            view.setTag(viewOfItem);
        }else {
            viewOfItem = (ViewOfItem) view.getTag();
        }
        viewOfItem.tvTenLoai.setText(list.get(i).getTenLoai());
        return view;
    }

    public static class ViewOfItem {
        TextView tvTenLoai;
    }
}
