package app.edu.app.ui;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;


import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import app.edu.app.R;
import app.edu.app.dao.HoaDonChiTietDAO;
import app.edu.app.model.HoaDonChiTiet;
import app.edu.app.utils.XDate;

public class DoanhThuActivity extends AppCompatActivity {
    Toolbar toolbar;
    TextView tvDoanhThuNam, tvDoanhThuThang, tvDoanhThuNgay, tvThang,tvNam;
    HoaDonChiTietDAO hoaDonChiTietDAO;
    CardView cardViewNgay, cardViewThang, cardViewNam;
    List<HoaDonChiTiet> hoaDonChiTietListDay = new ArrayList<>();
    List<HoaDonChiTiet> hoaDonChiTietListMonth = new ArrayList<>();
    List<HoaDonChiTiet> hoaDonChiTietListYear = new ArrayList<>();
    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doanh_thu);
        initToolBar();
        initView();

        hoaDonChiTietDAO = new HoaDonChiTietDAO(this);

        getDoanhThuNgay();
        getDoangThuThang();
        getDoanhThuNam();
        cardViewNgay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(DoanhThuActivity.this, TopSanPhamActivity.class);
                intent.putExtra("list", (ArrayList<HoaDonChiTiet>) hoaDonChiTietListDay);
                startActivity(intent);
            }
        });
        cardViewThang.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(DoanhThuActivity.this, TopSanPhamActivity.class);
                intent.putExtra("list", (ArrayList<HoaDonChiTiet>) hoaDonChiTietListMonth);
                startActivity(intent);
            }
        });
        cardViewNam.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(DoanhThuActivity.this, TopSanPhamActivity.class);
                intent.putExtra("list", (ArrayList<HoaDonChiTiet>) hoaDonChiTietListYear);
                startActivity(intent);
            }
        });

    }

    @SuppressLint("SetTextI18n")
    private void getDoangThuThang() {
        Calendar calendar = Calendar.getInstance();

        int monthNow = calendar.get(Calendar.MONTH);
        int yearNow = calendar.get(Calendar.YEAR);
        // date: 01-monthNow-yearNow
        calendar.set(yearNow, monthNow, 1);
        Date tuNgay = calendar.getTime();
        // date : getDate()-monthNow-yearNow
        calendar.set(yearNow, monthNow, getDate(monthNow, yearNow));
        Date denNgay = calendar.getTime();
        tvThang.setText("Tháng " + (monthNow + 1));
        tvDoanhThuThang.setText(hoaDonChiTietDAO.getDTThangNam(
                XDate.toStringDate(tuNgay),
                XDate.toStringDate(denNgay)) + "VND");
        hoaDonChiTietListMonth = hoaDonChiTietDAO.getTopSellingProductsInMonth(XDate.toStringDate(tuNgay), XDate.toStringDate(denNgay));
        Log.e("getDoangThuThang: ", String.valueOf(hoaDonChiTietListMonth.size()));
    }

    // Trả về số ngày theo tháng và năm nhuận
    private int getDate(int moth, int year) {
        switch (moth) {
            case 0: // tháng 1
            case 2: // tháng 3
            case 4: // tháng 5
            case 6: // tháng 7
            case 7: // tháng 8
            case 9: // tháng 9
            case 11:// tháng 12
                return 31;
            case 3: // tháng 4
            case 5: // tháng 6
            case 8: // tháng 9
            case 10: // tháng 11
                return 30;
            case 1:// tháng 2
                if ((year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)) {
                    return 29;
                } else {
                    return 28;
                }
        }
        return 0;
    }

    // Tính doanh thu theo năm
    @SuppressLint("SetTextI18n")
    private void getDoanhThuNam() {
        Calendar calendar = Calendar.getInstance();
        int yearNow = calendar.get(Calendar.YEAR);
        // date: 01-01-yearNow
        calendar.set(yearNow, 0, 1);
        Date tuNgay = calendar.getTime();
        // date: 31-12-yearNow
        calendar.set(yearNow, 11, 31);
        Date denNgay = calendar.getTime();

        tvDoanhThuNam.setText(hoaDonChiTietDAO.getDTThangNam(
                XDate.toStringDate(tuNgay),
                XDate.toStringDate(denNgay)) + "VND"
        );
        tvNam.setText("Năm " + yearNow);
        hoaDonChiTietListYear = hoaDonChiTietDAO.getTopSellingProductsInMonth(XDate.toStringDate(tuNgay), XDate.toStringDate(denNgay));
        Log.e("getDoanhThuNam: ", String.valueOf(hoaDonChiTietListYear.size()));
    }

    // Tính doang thu theo ngày
    @SuppressLint("SetTextI18n")
    private void getDoanhThuNgay() {
        Date dateNow = Calendar.getInstance().getTime();
        int caculatorDoanhthu = hoaDonChiTietDAO.getDoanhThuNgay(XDate.toStringDate(dateNow));
        hoaDonChiTietListDay = hoaDonChiTietDAO.getTop5BestSellingProducts(XDate.toStringDate(dateNow));
        tvDoanhThuNgay.setText(caculatorDoanhthu + "VND");
        Log.e("getDoanhThuNgay: ", String.valueOf(hoaDonChiTietListDay.size()));
    }

    private void initView() {
        tvDoanhThuNam = findViewById(R.id.tvDoanhThuNam);
        tvDoanhThuThang = findViewById(R.id.tvDoanhThuThang);
        tvDoanhThuNgay = findViewById(R.id.tvDoanhThuNgay);
        tvThang = findViewById(R.id.tvThang);
        tvNam = findViewById(R.id.tvNam);
        cardViewNgay = findViewById(R.id.cardViewNgay);
        cardViewThang = findViewById(R.id.cardViewThang);
        cardViewNam = findViewById(R.id.cardViewNam);
    }

    private void initToolBar() {
        toolbar = findViewById(R.id.toolbarDoanhThu);
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
}