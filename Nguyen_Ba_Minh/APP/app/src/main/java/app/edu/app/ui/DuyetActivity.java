package app.edu.app.ui;

import static app.edu.app.model.HoaDonMangVe.CHUA_DUYET;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import java.util.Calendar;

import app.edu.app.R;
import app.edu.app.adapter.DuyetAdapter;
import app.edu.app.dao.HoaDonMangVeDao;
import app.edu.app.interfaces.ItemHoaDonMangVeOnClick;
import app.edu.app.model.HoaDonMangVe;

public class DuyetActivity extends AppCompatActivity{
    public static final String MA_HOA_DON = "maHoaDon";
    Toolbar toolbar;
    RecyclerView recyclerViewHoaDon;
    HoaDonMangVeDao hoaDonDAO;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_duyet);
        initToolBar();

        initView();
        hoaDonDAO = new HoaDonMangVeDao(this);
        loadData();
    }


    private void loadData() {
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this, RecyclerView.VERTICAL, false);
        recyclerViewHoaDon.setLayoutManager(linearLayoutManager);
        DuyetAdapter adapter = new DuyetAdapter(hoaDonDAO.getByTrangThai(CHUA_DUYET), DuyetActivity.this, new ItemHoaDonMangVeOnClick() {
            @Override
            public void itemOclick(View view, HoaDonMangVe hoaDonMangVe) {
                Intent intent = new Intent(DuyetActivity.this, ChiTietHoaDonMangVeActivity.class);
                intent.putExtra(MA_HOA_DON, String.valueOf(hoaDonMangVe.getMaHoaDon()));
                startActivity(intent);
                overridePendingTransition(R.anim.anim_in_right, R.anim.anim_out_left);
            }

            @Override
            public void itemDuyet(View view, HoaDonMangVe hoaDonMangVe) {
                Calendar c = Calendar.getInstance();
                hoaDonMangVe.setTrangThai(HoaDonMangVe.DA_DUYET);
                hoaDonMangVe.setGioRa(c.getTime());
                HoaDonMangVeDao hoaDonMangVeDao = new HoaDonMangVeDao(DuyetActivity.this);
                hoaDonMangVeDao.updateHoaDonMangVe(hoaDonMangVe);
                loadData();
            }
            @Override
            public void itemHuy(View view, HoaDonMangVe hoaDonMangVe) {
                hoaDonMangVe.setTrangThai(HoaDonMangVe.HUY_HOA_DON);
                hoaDonDAO.updateHoaDonMangVe(hoaDonMangVe);
                loadData();
            }
        });
        recyclerViewHoaDon.setAdapter(adapter);
    }

    private void initView() {
        recyclerViewHoaDon = findViewById(R.id.recyclerViewHoaDon);
    }

    private void initToolBar() {
        toolbar = findViewById(R.id.toolbarHoaDon);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.anim_in_left, R.anim.anim_out_right);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData();
    }

}