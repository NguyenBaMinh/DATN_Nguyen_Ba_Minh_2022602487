package app.edu.app.ui;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;

import app.edu.app.R;
import app.edu.app.adapter.BanAdapter;
import app.edu.app.dao.BanDAO;
import app.edu.app.dao.HoaDonDAO;
import app.edu.app.dao.NguoiDungDAO;
import app.edu.app.interfaces.ItemBanOnClick;
import app.edu.app.model.Ban;
import app.edu.app.model.HoaDon;
import app.edu.app.model.NguoiDung;
import app.edu.app.utils.MyToast;

/**
 * Activity quản lý bàn cho nhân viên/admin
 * 
 * MỤC ĐÍCH:
 * - Quản lý trạng thái HIỆN TẠI của bàn (khách đang ngồi hay không)
 * - Tạo hóa đơn khi khách đến
 * - Xem hóa đơn khi bàn có khách
 * 
 * LOGIC:
 * - Ban.trangThai: Phản ánh trạng thái HIỆN TẠI
 *   → 0 (CON_TRONG): Bàn trống → Có thể tạo hóa đơn
 *   → 1 (CO_KHACH): Bàn có khách → Xem hóa đơn
 * 
 * KHÁC BIỆT VỚI DatBanActivity:
 * - QuanLyBanActivity: Quản lý trạng thái hiện tại (khách đang ngồi)
 * - DatBanActivity: Đặt bàn cho tương lai (dựa vào DatBan)
 */
public class QuanLyBanActivity extends AppCompatActivity {
    public static final String MA_BAN = "maBan";
    Toolbar toolbar;
    RecyclerView recyclerViewBan;
    BanDAO banDAO;
    HoaDonDAO hoaDonDAO;
    private SharedPreferences sharedPreferences;
    private BanAdapter banAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quan_ly_ban);
        initToolBar();
        initView();
        sharedPreferences = getSharedPreferences("USER_FILE", MODE_PRIVATE);
        banDAO = new BanDAO(this);
        hoaDonDAO = new HoaDonDAO(this);
        setupFirebaseListener();
    }

    private void initView() {
        recyclerViewBan = findViewById(R.id.recyclerViewBan);
    }

    private void initToolBar() {
        toolbar = findViewById(R.id.toolbarBan);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
    }

    private void setupFirebaseListener() {
        // Setup layout manager
        LinearLayoutManager linearLayoutManager = new GridLayoutManager(this, 3);
        recyclerViewBan.setLayoutManager(linearLayoutManager);
        
        // Tạo adapter với empty list ban đầu
        // isDatBanContext = false: Dùng logic dựa vào Ban.trangThai (trạng thái hiện tại)
        banAdapter = new BanAdapter(new ArrayList<>(), new ItemBanOnClick() {
            @Override
            public void itemOclick(View view, Ban ban) {
                if (ban.getTrangThai() == Ban.CON_TRONG) {
                    createNewHoaDon(ban);
                } else {
                    Intent intent = new Intent(QuanLyBanActivity.this, OderActivity.class);
                    intent.putExtra(MA_BAN, String.valueOf(ban.getMaBan()));
                    startActivity(intent);
                    overridePendingTransition(R.anim.anim_in_right, R.anim.anim_out_left);
                    finish();
                }
            }
        });
        recyclerViewBan.setAdapter(banAdapter);
        
        // Lắng nghe real-time từ Firebase
        banDAO.getAllFromFirebaseDirect(new BanDAO.OnBanListListener() {
            @Override
            public void onBanListReceived(ArrayList<Ban> banList) {
                // Cập nhật adapter với danh sách mới từ Firebase
                banAdapter.updateList(banList);
            }

            @Override
            public void onError(Exception e) {
                // Xử lý lỗi nếu cần
            }
        });
    }

    private void createNewHoaDon(Ban ban) {
        // tạo hoá đơn mới
        View viewDialog = LayoutInflater.from(QuanLyBanActivity.this).inflate(R.layout.layout_dialog_oder, null);
        Button btnOder = viewDialog.findViewById(R.id.btnOder);
        TextView tvBoQua = viewDialog.findViewById(R.id.tvBoQua);

        Dialog dialog = new Dialog(QuanLyBanActivity.this);
        dialog.setContentView(viewDialog);

        tvBoQua.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });

        btnOder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Cập nhật trạng thái bàn thành "Có khách" khi khách đến và tạo hóa đơn
                // Đây là trạng thái HIỆN TẠI, khác với đặt bàn cho tương lai (DatBan)
                ban.setTrangThai(Ban.CO_KHACH);
                if (banDAO.updateBan(ban)) {
                    // Không cần gọi loadData() vì Firebase listener sẽ tự động cập nhật
                    // Tạo hóa đơn mới
                    Calendar c = Calendar.getInstance(); // lấy ngày tháng năm và giờ hiện tại
                    HoaDon hoaDon = new HoaDon();
                    hoaDon.setMaBan(ban.getMaBan());
                    hoaDon.setGioVao(c.getTime());
                    hoaDon.setGioRa(c.getTime());
                    hoaDon.setTrangThai(HoaDon.CHUA_THANH_TOAN);
                    String maNguoiDung = sharedPreferences.getString("maNguoiDung", "");
                    hoaDon.setMaKhachHang(maNguoiDung);
                    hoaDon.setGhiChu("");
                    hoaDonDAO.insertHoaDon(hoaDon);
                    dialog.dismiss();
                }
            }
        });
        dialog.show();
        dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        dialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
        dialog.getWindow().setGravity(Gravity.BOTTOM);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.anim_in_left, R.anim.anim_out_right);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_add, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menu_add) {
            NguoiDungDAO nguoiDungDAO = new NguoiDungDAO(this);
            String maNguoiDung = sharedPreferences.getString("maNguoiDung", "");
            NguoiDung nguoiDung = nguoiDungDAO.getByMaNguoiDung(maNguoiDung);
            if(nguoiDung.isAdmin()){
                addBan();
            }else{
                MyToast.error(QuanLyBanActivity.this, "Bạn không có quyền thêm bàn");
            }

        }
        return super.onOptionsItemSelected(item);
    }

    private void addBan() {
        // Thêm bàn mới
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogTheme);
        builder.setMessage("Bạn có muốn thêm bàn mới?");
        builder.setPositiveButton("Có", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Ban ban = new Ban();
                ban.setTrangThai(Ban.CON_TRONG);
                if (banDAO.insertBan(ban)) {
                    // Không cần gọi loadData() vì Firebase listener sẽ tự động cập nhật
                    MyToast.successful(QuanLyBanActivity.this, "Thêm bàn mới thành công");
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

    @Override
    protected void onResume() {
        super.onResume();
        // Không cần load lại vì Firebase listener đang chạy real-time
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Dừng listener khi activity bị destroy
        banDAO.stopFirebaseDirectListener();
    }
}