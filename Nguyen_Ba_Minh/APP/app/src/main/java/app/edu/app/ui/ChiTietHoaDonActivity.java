package app.edu.app.ui;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import app.edu.app.R;
import app.edu.app.adapter.ChiTietHoaDonAdapter;
import app.edu.app.dao.HangHoaDAO;
import app.edu.app.dao.HoaDonChiTietDAO;
import app.edu.app.dao.HoaDonDAO;
import app.edu.app.dao.HoaDonMangVeDao;
import app.edu.app.dao.NguoiDungDAO;
import app.edu.app.model.HangHoa;
import app.edu.app.model.HoaDon;
import app.edu.app.model.HoaDonChiTiet;
import app.edu.app.model.HoaDonMangVe;
import app.edu.app.model.NguoiDung;
import app.edu.app.utils.MyToast;
import app.edu.app.utils.XDate;

public class ChiTietHoaDonActivity extends AppCompatActivity {
    RecyclerView recyclerViewThucUong;
    HoaDonChiTietDAO hoaDonChiTietDAO;
    HangHoaDAO hangHoaDAO;
    HoaDonDAO hoaDonDAO;
    HoaDonMangVeDao hoaDonMangVeDao;
    NguoiDungDAO nguoiDungDAO;
    TextView tvMaBan, tvGioVao, tvGioRa, tvMaBana, tvGhiChu;
    ImageView ivBack;
    Button btnDelete;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chi_tiet_hoa_don);
        initView();
        hoaDonChiTietDAO = new HoaDonChiTietDAO(this);
        hangHoaDAO = new HangHoaDAO(this);
        hoaDonDAO = new HoaDonDAO(this);
        nguoiDungDAO = new NguoiDungDAO(this);
        hoaDonMangVeDao = new HoaDonMangVeDao(this);
        loadData();
        fillActivity();

        ivBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showComfirmDeleteHDCT();
            }
        });

    }

    private void showComfirmDeleteHDCT() {
        // Xoá hoá đơn chi tiết và hoá đơn (Theo mã hoá đơn)
        AlertDialog.Builder builder = new AlertDialog.Builder(ChiTietHoaDonActivity.this, R.style.AlertDialogTheme)
                .setMessage("Bạn có muốn xoá hoá đơn HD0775098507" + getMaHoaDon() + "?")
                .setPositiveButton("Xoá", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (hoaDonDAO.deleteHoaDon(getMaHoaDon()) && hoaDonChiTietDAO.deleteHoaDonChiTietByMaHoaDon(getMaHoaDon())) {
                            MyToast.successful(ChiTietHoaDonActivity.this, "Xoá thành công");
                            onBackPressed();
                        } else {
                            MyToast.error(ChiTietHoaDonActivity.this, "Xoá không thành côn");
                        }
                    }
                })
                .setNegativeButton("Huỷ", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @SuppressLint("SetTextI18n")
    private void fillActivity() {
        try {
            String maKhachHang = getMaKhachHang();
            if (maKhachHang == null || maKhachHang.isEmpty()) {
                Log.e("ChiTietHoaDon", "maKhachHang is null or empty");
                MyToast.error(this, "Không tìm thấy thông tin khách hàng");
                finish();
                return;
            }
            
            NguoiDung nguoiDung = nguoiDungDAO.getByMaNguoiDung(maKhachHang);
            if (nguoiDung == null) {
                Log.e("ChiTietHoaDon", "NguoiDung not found for: " + maKhachHang);
                MyToast.error(this, "Không tìm thấy thông tin khách hàng");
                finish();
                return;
            }
            
            Log.e("maBan", getMaBan() + "");
            
            if (getMaBan() == -1) {
                HoaDonMangVe hoaDonMangVe = hoaDonMangVeDao.getByMaHoaDon(getMaHoaDon());
                if (hoaDonMangVe != null) {
                    tvMaBana.setText("Tên KH: ");
                    tvMaBan.setText("" + nguoiDung.getHoVaTen());
                    tvGioVao.setText(XDate.toStringDateTime(hoaDonMangVe.getGioVao()));
                    tvGioRa.setText(XDate.toStringDateTime(hoaDonMangVe.getGioRa()));
                    tvGhiChu.setText("Ghi chú: " + (hoaDonMangVe.getGhiChu() != null ? hoaDonMangVe.getGhiChu() : ""));
                }
            } else {
                HoaDon hoaDon = hoaDonDAO.getByMaHoaDon(getMaHoaDon());
                if (hoaDon != null) {
                    tvMaBan.setText("B0" + hoaDon.getMaBan() + " <--->KH " + nguoiDung.getHoVaTen());
                    tvGioVao.setText(XDate.toStringDateTime(hoaDon.getGioVao()));
                    tvGioRa.setText(XDate.toStringDateTime(hoaDon.getGioRa()));
                    tvGhiChu.setText("Ghi chú: " + (hoaDon.getGhiChu() != null ? hoaDon.getGhiChu() : ""));
                }
            }
        } catch (Exception e) {
            Log.e("ChiTietHoaDon", "Error in fillActivity: " + e.getMessage(), e);
            MyToast.error(this, "Lỗi tải thông tin hóa đơn");
        }
    }

    private String getMaHoaDon() {
        Intent intent = getIntent();
        return intent.getStringExtra(HoaDonActivity.MA_HOA_DON);
    }

    private String getMaKhachHang() {
        Intent intent = getIntent();
        return intent.getStringExtra("maKhachHang");
    }

    private int getMaBan() {
        Intent intent = getIntent();
        return intent.getIntExtra("maBan", -1);
    }

    private void loadData() {
        try {
            String maHoaDon = getMaHoaDon();
            if (maHoaDon == null || maHoaDon.isEmpty()) {
                Log.e("ChiTietHoaDon", "maHoaDon is null or empty");
                MyToast.error(this, "Không tìm thấy thông tin hóa đơn");
                finish();
                return;
            }
            
            Log.d("ChiTietHoaDon", "Loading invoice details for: " + maHoaDon);
            
            ArrayList<HoaDonChiTiet> listHDCT = hoaDonChiTietDAO.getByMaHoaDon(maHoaDon);
            
            if (listHDCT == null) {
                listHDCT = new ArrayList<>();
            }
            
            Log.d("ChiTietHoaDon", "Found " + listHDCT.size() + " invoice details");
            
            ArrayList<HangHoa> list = new ArrayList<>();
            for (int i = 0; i < listHDCT.size(); i++) {
                try {
                    HangHoa hangHoa = hangHoaDAO.getByMaHangHoa(String.valueOf(listHDCT.get(i).getMaHangHoa()));
                    if (hangHoa != null) {
                        list.add(hangHoa);
                    } else {
                        Log.w("ChiTietHoaDon", "HangHoa not found for id: " + listHDCT.get(i).getMaHangHoa());
                    }
                } catch (Exception e) {
                    Log.e("ChiTietHoaDon", "Error loading HangHoa: " + e.getMessage(), e);
                }
            }
            
            Log.d("ChiTietHoaDon", "Loaded " + list.size() + " products");
            
            LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
            recyclerViewThucUong.setLayoutManager(linearLayoutManager);
            
            ChiTietHoaDonAdapter adapter = new ChiTietHoaDonAdapter(this, list, listHDCT);
            recyclerViewThucUong.setAdapter(adapter);
            
            if (list.isEmpty()) {
                MyToast.error(this, "Hóa đơn không có sản phẩm nào");
            }
        } catch (Exception e) {
            Log.e("ChiTietHoaDon", "Error in loadData: " + e.getMessage(), e);
            MyToast.error(this, "Lỗi tải chi tiết hóa đơn");
        }
    }

    private void initView() {
        recyclerViewThucUong = findViewById(R.id.recyclerViewThucUong);
        tvMaBan = findViewById(R.id.tvMaBan);
        tvGioVao = findViewById(R.id.tvGioVao);
        tvGioRa = findViewById(R.id.tvGioRa);
        ivBack = findViewById(R.id.ivBack);
        btnDelete = findViewById(R.id.btnDelete);
        tvMaBana = findViewById(R.id.tvMaBana);
        tvGhiChu = findViewById(R.id.tvGhiChu);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.anim_in_left, R.anim.anim_out_right);
    }
}