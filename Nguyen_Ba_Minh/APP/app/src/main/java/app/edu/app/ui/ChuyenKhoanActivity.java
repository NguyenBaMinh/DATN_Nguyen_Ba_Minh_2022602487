package app.edu.app.ui;

import static app.edu.app.ui.OderActivity.maBan;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import java.util.Calendar;

import app.edu.app.R;
import app.edu.app.dao.BanDAO;
import app.edu.app.dao.HoaDonChiTietDAO;
import app.edu.app.dao.HoaDonDAO;
import app.edu.app.dao.HoaDonMangVeDao;
import app.edu.app.dao.ThongBaoDAO;
import app.edu.app.model.Ban;
import app.edu.app.model.HoaDon;
import app.edu.app.model.HoaDonMangVe;
import app.edu.app.model.ThongBao;
import app.edu.app.notification.MyNotification;
import app.edu.app.utils.MyToast;

public class ChuyenKhoanActivity extends AppCompatActivity {
    TextView tvctieptuc,tvchuyenkhoan;
    HoaDonDAO hoaDonDAO;
    HoaDonChiTietDAO hoaDonChiTietDAO;
    BanDAO banDAO;
    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_chuyenkhoan);

        tvctieptuc = findViewById(R.id.tvctieptuc);
        tvchuyenkhoan = findViewById(R.id.tvchuyenkhoan);

        hoaDonDAO = new HoaDonDAO(this);
        hoaDonChiTietDAO = new HoaDonChiTietDAO(this);
        banDAO = new BanDAO(this);
        HoaDon hoaDon = (HoaDon) getIntent().getSerializableExtra("hoaDon");
        Ban ban = (Ban) getIntent().getSerializableExtra("ban");
        if(ban == null || hoaDon == null){
            HoaDonMangVe hoaDonMangVe = (HoaDonMangVe) getIntent().getSerializableExtra("hoaDonMangVe");
            HoaDonMangVeDao hoaDonMangVeDao = new HoaDonMangVeDao(this);
            assert hoaDonMangVe != null;
            tvchuyenkhoan.setText(hoaDonMangVeDao.getGiaTien(hoaDonMangVe.getMaHoaDon())+"");
            tvctieptuc.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    hoaDonMangVeDao.updateHoaDonMangVe(hoaDonMangVe);
                    onBackPressed();
                }
            });
        }else {
            assert hoaDon != null;
            tvchuyenkhoan.setText(hoaDonChiTietDAO.getGiaTien(hoaDon.getMaHoaDon()) + "");

            tvctieptuc.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Calendar calendar = Calendar.getInstance();
                    if (banDAO.updateBan(ban) && hoaDonDAO.updateHoaDon(hoaDon)) {
                        MyToast.successful(ChuyenKhoanActivity.this, "Thanh Toán thành công");
                        MyNotification.getNotification(ChuyenKhoanActivity.this, "Thanh toán thành công hoá đơn HD0775098507" + hoaDon.getMaHoaDon());
                        themThonBaoMoi(hoaDon, calendar);
                    }
                    Intent intent = new Intent(ChuyenKhoanActivity.this, QuanLyBanActivity.class);
                    maBan = "";
                    startActivity(intent);
                    finish();
                }
            });
        }
    }

    private void themThonBaoMoi(HoaDon hoaDon, Calendar calendar) {
        // Tạo thông báo thanh toán hoá đơn
        ThongBao thongBao = new ThongBao();
        thongBao.setNoiDung("Thanh toán thành công hoá đơn HD0775098507"+ hoaDon.getMaHoaDon());
        thongBao.setTrangThai(ThongBao.STATUS_CHUA_XEM);
        thongBao.setNgayThongBao(calendar.getTime());
        ThongBaoDAO thongBaoDAO = new ThongBaoDAO(ChuyenKhoanActivity.this);
        thongBaoDAO.insertThongBao(thongBao);
    }
}