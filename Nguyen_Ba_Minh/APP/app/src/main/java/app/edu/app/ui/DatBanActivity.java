package app.edu.app.ui;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

import app.edu.app.R;
import app.edu.app.adapter.BanAdapter;
import app.edu.app.dao.BanDAO;
import app.edu.app.dao.DatBanDAO;
import app.edu.app.dao.NguoiDungDAO;
import app.edu.app.interfaces.ItemBanOnClick;
import app.edu.app.model.Ban;
import app.edu.app.model.DatBan;
import app.edu.app.model.NguoiDung;
import app.edu.app.utils.MyToast;
import app.edu.app.utils.XDate;

/**
 * Activity đặt bàn
 * 
 * MỐI QUAN HỆ GIỮA BAN VÀ DATBAN:
 * 
 * 1. BẢNG BAN:
 *    - Chỉ dùng để HIỂN THỊ danh sách các bàn đang có trong hệ thống
 *    - Ban.trangThai: Phản ánh trạng thái hiện tại (0=trống, 1=có khách)
 *    - KHÔNG dùng để xác định bàn đã được đặt hay chưa
 * 
 * 2. BẢNG DATBAN:
 *    - Dùng để XÁC ĐỊNH bàn đã được đặt hay chưa
 *    - DatBan.trangThai: 0=chờ duyệt, 1=đã duyệt, -1=từ chối
 *    - Kiểm tra DatBan để biết bàn nào đã được đặt trong ngày đã chọn
 * 
 * LOGIC HIỂN THỊ BÀN:
 * - Ban: Chỉ để lấy danh sách các bàn (maBan)
 * - DatBan: Dùng để xác định bàn nào đã được đặt (bookedTableIds)
 * - Bỏ qua hoàn toàn Ban.trangThai trong logic đặt bàn
 */
public class DatBanActivity extends AppCompatActivity {
    private Toolbar toolbar;
    private TextView tvNgayGioSuDung;
    private RecyclerView recyclerViewBan;
    private EditText edtGhiChu;
    private Button btnGuiYeuCau;
    
    private BanDAO banDAO;
    private DatBanDAO datBanDAO;
    private NguoiDungDAO nguoiDungDAO;
    private SharedPreferences sharedPreferences;
    
    private BanAdapter banAdapter;
    private ArrayList<Ban> banList;
    private Set<Integer> bookedTableIds; // Danh sách mã bàn đã được đặt trong ngày đã chọn
    
    private Calendar selectedDateTime; // Ngày giờ đã chọn để sử dụng bàn
    private Ban selectedBan; // Bàn đã chọn
    private String maKhachHang;
    private String tenKhachHang;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dat_ban);
        
        initView();
        initToolBar();
        initData();
        setupRecyclerView();
        setupListeners();
        loadTables();
    }
    
    private void initView() {
        toolbar = findViewById(R.id.toolbarDatBan);
        tvNgayGioSuDung = findViewById(R.id.tvNgayGioSuDung);
        recyclerViewBan = findViewById(R.id.recyclerViewBan);
        edtGhiChu = findViewById(R.id.edtGhiChu);
        btnGuiYeuCau = findViewById(R.id.btnGuiYeuCau);
    }
    
    private void initToolBar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
    }
    
    private void initData() {
        banDAO = new BanDAO(this);
        datBanDAO = new DatBanDAO(this);
        nguoiDungDAO = new NguoiDungDAO(this);
        sharedPreferences = getSharedPreferences("USER_FILE", MODE_PRIVATE);
        
        banList = new ArrayList<>();
        bookedTableIds = new HashSet<>();
        selectedDateTime = Calendar.getInstance();
        selectedBan = null;
        
        // Lấy thông tin người dùng hiện tại
        maKhachHang = sharedPreferences.getString("maNguoiDung", "");
        if (!maKhachHang.isEmpty()) {
            try {
                NguoiDung nguoiDung = nguoiDungDAO.getByMaNguoiDung(maKhachHang);
                if (nguoiDung != null) {
                    tenKhachHang = nguoiDung.getHoVaTen();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    private void setupRecyclerView() {
        GridLayoutManager layoutManager = new GridLayoutManager(this, 3);
        recyclerViewBan.setLayoutManager(layoutManager);
        
        // isDatBanContext = true: Dùng logic dựa vào DatBan để xác định bàn đã đặt
        banAdapter = new BanAdapter(banList, new ItemBanOnClick() {
            @Override
            public void itemOclick(View view, Ban ban) {
                // Chọn bàn
                if (selectedBan != null && selectedBan.getMaBan() == ban.getMaBan()) {
                    // Bỏ chọn nếu đã chọn rồi
                    selectedBan = null;
                    banAdapter.setSelectedTableId(null); // Bỏ highlight
                } else {
                    // Chọn bàn mới
                    selectedBan = ban;
                    banAdapter.setSelectedTableId(ban.getMaBan()); // Highlight bàn được chọn
                    MyToast.successful(DatBanActivity.this, "Đã chọn bàn " + ban.getMaBan());
                }
            }
        }, true); // true = màn hình đặt bàn
        recyclerViewBan.setAdapter(banAdapter);
    }
    
    private void updateSelectedTable(View view) {
        // Không cần nữa - đã xử lý trong setSelectedTableId
    }
    
    private void setupListeners() {
        // Chọn ngày giờ sử dụng
        tvNgayGioSuDung.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDateTimePicker();
            }
        });
        
        // Gửi yêu cầu đặt bàn
        btnGuiYeuCau.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                submitBookingRequest();
            }
        });
    }
    
    private void showDateTimePicker() {
        // Hiển thị DatePicker trước
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        
        DatePickerDialog datePickerDialog = new DatePickerDialog(
            this,
            R.style.MyDatePickerDialogTheme,
            new DatePickerDialog.OnDateSetListener() {
                @Override
                public void onDateSet(DatePicker datePicker, int y, int m, int d) {
                    selectedDateTime.set(Calendar.YEAR, y);
                    selectedDateTime.set(Calendar.MONTH, m);
                    selectedDateTime.set(Calendar.DAY_OF_MONTH, d);
                    
                    // Sau khi chọn ngày, hiển thị TimePicker
                    showTimePicker();
                }
            },
            year, month, day
        );
        
        // Chỉ cho phép chọn ngày từ hôm nay trở đi
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePickerDialog.show();
    }
    
    private void showTimePicker() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        
        TimePickerDialog timePickerDialog = new TimePickerDialog(
            this,
            new TimePickerDialog.OnTimeSetListener() {
                @Override
                public void onTimeSet(TimePicker timePicker, int h, int m) {
                    selectedDateTime.set(Calendar.HOUR_OF_DAY, h);
                    selectedDateTime.set(Calendar.MINUTE, m);
                    selectedDateTime.set(Calendar.SECOND, 0);
                    
                    // Cập nhật TextView hiển thị ngày giờ đã chọn
                    String dateTimeStr = XDate.toStringDateTime(selectedDateTime.getTime());
                    tvNgayGioSuDung.setText(dateTimeStr);
                    
                    // Lọc lại danh sách bàn đã được đặt trong ngày này
                    filterBookedTables(dateTimeStr);
                }
            },
            hour, minute, true // 24-hour format
        );
        
        timePickerDialog.show();
    }
    
    /**
     * Lọc danh sách bàn đã được đặt trong ngày đã chọn
     * 
     * LOGIC:
     * - Chỉ dựa vào DatBan để xác định bàn đã được đặt
     * - Ban chỉ dùng để hiển thị danh sách bàn, không dùng để xác định trạng thái đặt
     * 
     * @param ngayGioSuDung Ngày giờ sử dụng bàn (format: "dd-MM-yyyy HH:mm:ss")
     */
    private void filterBookedTables(String ngayGioSuDung) {
        // Lấy danh sách bàn đã được đặt trong ngày đã chọn từ DatBan
        ArrayList<DatBan> datBanList = datBanDAO.getDaDuyetByNgayGioSuDung(ngayGioSuDung);
        
        bookedTableIds.clear();
        for (DatBan datBan : datBanList) {
            // Chỉ lấy những đặt bàn đã duyệt hoặc đang chờ duyệt
            // Bàn đã từ chối (-1) hoặc đã hủy (3) không tính là đã đặt
            if (datBan.getTrangThai() == DatBan.TRANG_THAI_DA_DUYET || 
                datBan.getTrangThai() == DatBan.TRANG_THAI_CHO_DUYET) {
                bookedTableIds.add(datBan.getMaBan());
            }
        }
        
        // Cập nhật adapter với danh sách bàn đã đặt (từ DatBan)
        banAdapter.setBookedTableIds(bookedTableIds);
        
        // Log để debug
        android.util.Log.d("DatBanActivity", "Ngày đã chọn: " + ngayGioSuDung);
        android.util.Log.d("DatBanActivity", "Số bàn đã đặt trong ngày này (từ DatBan): " + bookedTableIds.size());
        android.util.Log.d("DatBanActivity", "Danh sách mã bàn đã đặt: " + bookedTableIds.toString());
    }
    
    /**
     * Load danh sách bàn từ Firebase
     * 
     * LƯU Ý:
     * - Ban chỉ dùng để lấy danh sách các bàn (maBan) - hiển thị danh sách bàn có trong hệ thống
     * - Ban.trangThai KHÔNG được dùng để xác định bàn đã đặt
     * - Việc xác định bàn đã đặt hoàn toàn dựa vào DatBan (thông qua filterBookedTables)
     */
    private void loadTables() {
        // Lấy danh sách bàn từ Firebase real-time (chỉ để lấy danh sách các bàn)
        banDAO.getAllFromFirebaseDirect(new BanDAO.OnBanListListener() {
            @Override
            public void onBanListReceived(ArrayList<Ban> banList) {
                DatBanActivity.this.banList = banList;
                banAdapter.updateList(banList);
                
                // Nếu đã chọn ngày giờ, lọc lại bàn đã đặt từ DatBan
                if (selectedDateTime != null && tvNgayGioSuDung.getText().toString().contains("-")) {
                    String dateTimeStr = tvNgayGioSuDung.getText().toString();
                    filterBookedTables(dateTimeStr);
                }
            }
            
            @Override
            public void onError(Exception e) {
                MyToast.error(DatBanActivity.this, "Lỗi khi tải danh sách bàn: " + e.getMessage());
            }
        });
    }
    
    private void submitBookingRequest() {
        // Kiểm tra đã chọn ngày giờ chưa
        if (tvNgayGioSuDung.getText().toString().equals("Chọn ngày giờ") || 
            tvNgayGioSuDung.getText().toString().isEmpty()) {
            MyToast.error(this, "Vui lòng chọn ngày giờ sử dụng bàn");
            return;
        }
        
        // Kiểm tra đã chọn bàn chưa
        if (selectedBan == null) {
            MyToast.error(this, "Vui lòng chọn bàn muốn đặt");
            return;
        }
        
        // Kiểm tra bàn đã được đặt chưa
        String ngayGioSuDung = tvNgayGioSuDung.getText().toString();
        if (datBanDAO.isBanDaDat(selectedBan.getMaBan(), ngayGioSuDung)) {
            MyToast.error(this, "Bàn " + selectedBan.getMaBan() + " đã được đặt trong thời gian này");
            return;
        }
        
        // Kiểm tra người dùng đã đăng nhập chưa
        if (maKhachHang == null || maKhachHang.isEmpty()) {
            MyToast.error(this, "Vui lòng đăng nhập để đặt bàn");
            return;
        }
        
        // Tạo đối tượng DatBan
        DatBan datBan = new DatBan();
        datBan.setMaBan(selectedBan.getMaBan());
        datBan.setMaKhachHang(maKhachHang);
        
        // Ngày giờ đặt (hiện tại)
        Calendar now = Calendar.getInstance();
        datBan.setNgayGioDat(XDate.toStringDateTime(now.getTime()));
        
        // Ngày giờ sử dụng (đã chọn)
        datBan.setNgayGioSuDung(ngayGioSuDung);
        
        // Trạng thái: Chờ duyệt
        datBan.setTrangThai(DatBan.TRANG_THAI_CHO_DUYET);
        
        // Ghi chú
        String ghiChu = edtGhiChu.getText().toString().trim();
        datBan.setGhiChu(ghiChu != null ? ghiChu : "");
        
        // Lưu đặt bàn
        if (datBanDAO.insertDatBan(datBan)) {
            MyToast.successful(this, "Đã gửi yêu cầu đặt bàn thành công! Vui lòng chờ admin duyệt.");
            
            // Reset form
            resetForm();
        } else {
            MyToast.error(this, "Có lỗi xảy ra khi đặt bàn. Vui lòng thử lại.");
        }
    }
    
    private void resetForm() {
        // Reset các trường
        tvNgayGioSuDung.setText("Chọn ngày giờ");
        edtGhiChu.setText("");
        selectedBan = null;
        selectedDateTime = Calendar.getInstance();
        bookedTableIds.clear();
        banAdapter.setBookedTableIds(bookedTableIds);
        banAdapter.setSelectedTableId(null); // Clear highlight
        banAdapter.notifyDataSetChanged();
    }
    
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.anim_in_left, R.anim.anim_out_right);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Dừng listener Firebase khi activity bị destroy
        if (banDAO != null) {
            banDAO.stopFirebaseDirectListener();
        }
    }
}

