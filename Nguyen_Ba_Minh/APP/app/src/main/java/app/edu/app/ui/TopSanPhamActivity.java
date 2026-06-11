package app.edu.app.ui;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;

import java.util.List;

import app.edu.app.R;
import app.edu.app.adapter.TopSanPhamAdapter;
import app.edu.app.model.HoaDonChiTiet;

public class TopSanPhamActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    List<HoaDonChiTiet> hoaDonChiTietList;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_top_san_pham);

        initView();
        hoaDonChiTietList = (List<HoaDonChiTiet>) getIntent().getSerializableExtra("list");
        TopSanPhamAdapter topSanPhamAdapter = new TopSanPhamAdapter(this, hoaDonChiTietList);
        recyclerView.setAdapter(topSanPhamAdapter);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
    }

    private void initView() {
        recyclerView = findViewById(R.id.recyclerViewTopSanPham);
    }
}