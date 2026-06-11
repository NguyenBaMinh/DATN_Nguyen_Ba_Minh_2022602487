package app.edu.app.ui;

import static app.edu.app.model.HoaDonMangVe.CHUA_DUYET;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;

import app.edu.app.R;
import app.edu.app.adapter.HoaDonChiTietMainAdapter;
import app.edu.app.dao.HangHoaDAO;
import app.edu.app.dao.HoaDonChiTietDAO;
import app.edu.app.dao.HoaDonMangVeDao;
import app.edu.app.dao.ThongBaoDAO;
import app.edu.app.interfaces.ItemTangGiamSoLuongOnClick;
import app.edu.app.model.HangHoa;
import app.edu.app.model.HoaDonChiTiet;
import app.edu.app.model.HoaDonMangVe;
import app.edu.app.model.ThongBao;
import app.edu.app.utils.MyToast;

public class MangVeActivity extends AppCompatActivity {
    public static final String MA_HOA_DON = "maHoaDon";
    HoaDonChiTietDAO hoaDonChiTietDAO;
    HangHoaDAO hangHoaDAO;
    HoaDonMangVeDao hoaDonDAO;
    TextView tvMaBan, tvGioVao, tvThemMon, tvTamTinh, tvHoaDonCuoi,tvnguoi;
    EditText edtGhiChu;
    RecyclerView recyclerViewThucUong;
    Button btnThanhToan;
    Toolbar toolbar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mang_ve);
        initToolbar();
        initview();

        hoaDonChiTietDAO = new HoaDonChiTietDAO(this);
        hangHoaDAO = new HangHoaDAO(this);
        hoaDonDAO = new HoaDonMangVeDao(this);
        fillActivity();
        loadData();
        tvThemMon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openThemMonActivity();
            }
        });
        btnThanhToan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //xác nhận hóa đơn chuyển về trạng thái chưa duyệt
                AlertDialog.Builder builder = new AlertDialog.Builder(MangVeActivity.this, R.style.AlertDialogTheme);
                builder.setMessage("Xác nhận đơn ?");
                builder.setPositiveButton("Xác nhận", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Log.e("TAG>>>>>>>", "onClick: "+getHoaDon().getTrangThai() );
                        HoaDonMangVe hoaDon = getHoaDon();
                        hoaDon.setTrangThai(HoaDonMangVe.CHUA_DUYET);
                        String ghiChu = edtGhiChu.getText().toString();
                        hoaDon.setGhiChu(ghiChu);
                        Log.e("TAG>>>>>>>", "onClick: "+getHoaDon().getTrangThai() );
                        if(hoaDonDAO.updateHoaDonMangVe(hoaDon)){
                            MyToast.successful(MangVeActivity.this, "Xác nhận thành công");
                            loadData();
                            fillActivity();
                            themThonBaoMoi(hoaDon, Calendar.getInstance());
                        }else {
                            MyToast.error(MangVeActivity.this, "Xác nhận không thành công");
                        }
                    }
                });
                builder.setNegativeButton("Hủy", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                });
                builder.show();
            }
        });
    }




    private void initview() {
        tvMaBan = findViewById(R.id.tvMaBan);
        tvGioVao = findViewById(R.id.tvGioVao);
        recyclerViewThucUong = findViewById(R.id.recyclerViewThucUong);
        tvThemMon = findViewById(R.id.tvThemMon);
        btnThanhToan = findViewById(R.id.btnThanhToan);
        tvTamTinh = findViewById(R.id.tvTamTinh);
        tvHoaDonCuoi = findViewById(R.id.tvHoaDonCuoi);
        tvnguoi = findViewById(R.id.tvnguoidat);
        edtGhiChu = findViewById(R.id.edtGhiChu);
    }

    private void initToolbar() {
        toolbar = findViewById(R.id.toolbarOder);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
    }
    private void openThemMonActivity() {
        // Mở màng hình thêm món
        HoaDonMangVe hoaDon = getHoaDon();
        Intent intent = new Intent(MangVeActivity.this, ThemMonActivity.class);
        intent.putExtra(MA_HOA_DON, String.valueOf(hoaDon.getMaHoaDon()));
        startActivity(intent);
    }

    private void themThonBaoMoi(HoaDonMangVe hoaDon, Calendar calendar) {
        // Tạo thông báo thanh toán hoá đơn
        ThongBao thongBao = new ThongBao();
        thongBao.setNoiDung("Xác nhận đơn hàng " + hoaDon.getMaHoaDon());
        thongBao.setTrangThai(ThongBao.STATUS_CHUA_XEM);
        thongBao.setNgayThongBao(calendar.getTime());
        ThongBaoDAO thongBaoDAO = new ThongBaoDAO(MangVeActivity.this);
        thongBaoDAO.insertThongBao(thongBao);
    }

    private void loadData() {
        HoaDonMangVe hoaDon = getHoaDon();
        ArrayList<HoaDonChiTiet> listHDCT = hoaDonChiTietDAO.getByMaHoaDon(String.valueOf(hoaDon.getMaHoaDon())); // lấy All hoá đơn chi tiết theo mã hoá đơn
        ArrayList<HangHoa> list = new ArrayList<>();
        for (int i = 0; i < listHDCT.size(); i++) {
            list.add(hangHoaDAO.getByMaHangHoa(String.valueOf(listHDCT.get(i).getMaHangHoa())));
        }
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        recyclerViewThucUong.setLayoutManager(linearLayoutManager);
        HoaDonChiTietMainAdapter adapter = new HoaDonChiTietMainAdapter(this, list, listHDCT, new ItemTangGiamSoLuongOnClick() {
            @Override
            public void itemOclick(View view, int indext, HoaDonChiTiet hoaDonChiTiet, HangHoa hangHoa) {
                hoaDonChiTiet.setSoLuong(indext);
                hoaDonChiTiet.setGiaTien(indext * hangHoa.getGiaTien());
                hoaDonChiTietDAO.updateHoaDonChiTiet(hoaDonChiTiet);
                fillActivity();
            }

            @Override
            public void itemOclickDeleteHDCT(View view, HoaDonChiTiet hoaDonChiTiet) {
                // Xoá oder
                AlertDialog.Builder builder = new AlertDialog.Builder(MangVeActivity.this, R.style.AlertDialogTheme);
                builder.setMessage("Xoá oder ?");
                builder.setPositiveButton("Xoá", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if(hoaDonChiTietDAO.deleteHoaDonChiTiet(String.valueOf(hoaDonChiTiet.getMaHDCT()))){
                            MyToast.successful(MangVeActivity.this, "Xoá oder thành công");
                            loadData();
                            fillActivity();
                        }else {
                            MyToast.error(MangVeActivity.this, "Xoá không thành công");
                        }
                    }
                });
                builder.setNegativeButton("Huỷ", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });
        recyclerViewThucUong.setAdapter(adapter);
    }

    @SuppressLint("SetTextI18n")
    private void fillActivity() {
        HoaDonMangVe hoaDon = getHoaDon();
        if(hoaDon.getTrangThai() == CHUA_DUYET){
            btnThanhToan.setText("Đang chờ duyệt");
        }
        tvTamTinh.setText(hoaDonChiTietDAO.getGiaTien(hoaDon.getMaHoaDon()) + "VND");
        tvHoaDonCuoi.setText(hoaDonChiTietDAO.getGiaTien(hoaDon.getMaHoaDon()) + "VND");
    }

    private HoaDonMangVe getHoaDon() {
       String maNguoiDung = getIntent().getStringExtra("maNguoiDung");
       return hoaDonDAO.getByMaHoaDonVaTrangThai(maNguoiDung);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData();
        fillActivity();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.anim_in_left, R.anim.anim_out_right);
    }
}