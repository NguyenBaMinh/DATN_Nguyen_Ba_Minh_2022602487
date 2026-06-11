package app.edu.app.ui;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;

import app.edu.app.R;
import app.edu.app.adapter.DatBanAdapter;
import app.edu.app.dao.DatBanDAO;
import app.edu.app.interfaces.ItemDatBanOnClick;
import app.edu.app.model.DatBan;

public class QuanLyBanNguoiDungActivity extends AppCompatActivity {
    Toolbar toolbar;
    RecyclerView recyclerViewDatBan;
    SwipeRefreshLayout swipeRefresh;
    TextView tvEmpty;
    DatBanDAO datBanDAO;
    String maNguoiDung;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quan_ly_ban_nguoi_dung);

        initToolBar();
        initView();
        
        sharedPreferences = getSharedPreferences("USER_FILE", MODE_PRIVATE);
        maNguoiDung = sharedPreferences.getString("maNguoiDung", "");
        
        Log.d("QuanLyBanNguoiDung", "onCreate: maNguoiDung = [" + maNguoiDung + "]");
        
        // Kiểm tra maNguoiDung hợp lệ
        if (maNguoiDung == null || maNguoiDung.trim().isEmpty()) {
            Log.e("QuanLyBanNguoiDung", "ERROR: maNguoiDung is null or empty!");
            tvEmpty.setVisibility(View.VISIBLE);
            tvEmpty.setText("Vui lòng đăng nhập để xem danh sách bàn");
            recyclerViewDatBan.setVisibility(View.GONE);
            return;
        }
        
        datBanDAO = new DatBanDAO(this);
        syncAndLoadData();
    }
    
    /**
     * Load dữ liệu TRỰC TIẾP từ Firebase (không sync về SQLite)
     */
    private void syncAndLoadData() {
        loadData(false);
    }
    
    /**
     * Load dữ liệu với option force từ server
     * @param forceFromServer True để force load từ server (bỏ qua cache)
     */
    private void syncAndLoadDataForceRefresh() {
        loadData(true);
    }

    /**
     * Load danh sách đặt bàn TRỰC TIẾP từ Firebase
     * @param forceFromServer True để force load từ server (bỏ qua cache)
     */
    private void loadData(boolean forceFromServer) {
        Log.d("QuanLyBanNguoiDung", "========== BẮT ĐẦU LOAD DATA ==========");
        Log.d("QuanLyBanNguoiDung", "maNguoiDung: [" + maNguoiDung + "]");
        Log.d("QuanLyBanNguoiDung", "forceFromServer: " + forceFromServer);
        
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this, RecyclerView.VERTICAL, false);
        recyclerViewDatBan.setLayoutManager(linearLayoutManager);
        
        if (forceFromServer) {
            Log.d("QuanLyBanNguoiDung", "🔄 FORCE RELOAD từ server (bỏ qua cache)...");
        } else {
            Log.d("QuanLyBanNguoiDung", "Đang gọi Firebase để load đặt bàn (có thể từ cache)...");
        }
        
        // Load TRỰC TIẾP từ Firebase (không dùng SQLite)
        datBanDAO.getByMaKhachHangDaDuyetFromFirebaseDirect(maNguoiDung, 
            new DatBanDAO.OnDatBanListListener() {
                @Override
                public void onListReceived(ArrayList<DatBan> datBanList) {
                    // Tắt loading indicator
                    swipeRefresh.setRefreshing(false);
                    
                    Log.d("QuanLyBanNguoiDung", "========== CALLBACK RECEIVED ==========");
                    Log.d("QuanLyBanNguoiDung", "✓ Đã nhận " + datBanList.size() + " đặt bàn từ Firebase");
                    
                    if (datBanList.isEmpty()) {
                        Log.d("QuanLyBanNguoiDung", "Danh sách trống - hiển thị thông báo empty");
                        recyclerViewDatBan.setVisibility(View.GONE);
                        tvEmpty.setVisibility(View.VISIBLE);
                    } else {
                        Log.d("QuanLyBanNguoiDung", "Hiển thị danh sách " + datBanList.size() + " đặt bàn");
                        recyclerViewDatBan.setVisibility(View.VISIBLE);
                        tvEmpty.setVisibility(View.GONE);
                        
                        DatBanAdapter adapter = new DatBanAdapter(datBanList, new ItemDatBanOnClick() {
                            @Override
                            public void itemOclick(View view, DatBan datBan) {
                                // Mở OderActivity với maBan, maKhachHang và ngayGioSuDung
                                // OderActivity sẽ tự động load hóa đơn trực tiếp từ Firebase theo ngày
                                Intent intent = new Intent(QuanLyBanNguoiDungActivity.this, OderActivity.class);
                                intent.putExtra(QuanLyBanActivity.MA_BAN, String.valueOf(datBan.getMaBan()));
                                // QUAN TRỌNG: Truyền maKhachHang để đảm bảo chỉ lấy hóa đơn của khách hàng này
                                intent.putExtra("maKhachHang", datBan.getMaKhachHang());
                                // QUAN TRỌNG: Truyền ngayGioSuDung để lọc đúng hóa đơn theo ngày
                                intent.putExtra("ngayGioSuDung", datBan.getNgayGioSuDung());
                                startActivity(intent);
                                overridePendingTransition(R.anim.anim_in_right, R.anim.anim_out_left);
                                finish();
                            }
                        });
                        recyclerViewDatBan.setAdapter(adapter);
                    }
                }

                @Override
                public void onError(Exception e) {
                    // Tắt loading indicator
                    swipeRefresh.setRefreshing(false);
                    
                    Log.e("QuanLyBanNguoiDung", "========== LỖI LOAD DỮ LIỆU ==========");
                    Log.e("QuanLyBanNguoiDung", "Lỗi load đặt bàn từ Firebase", e);
                    Log.e("QuanLyBanNguoiDung", "Error message: " + e.getMessage());
                    
                    recyclerViewDatBan.setVisibility(View.GONE);
                    tvEmpty.setVisibility(View.VISIBLE);
                    tvEmpty.setText("Không thể tải danh sách bàn. Vui lòng thử lại.");
                }
            }, forceFromServer);
    }

    private void initView() {
        recyclerViewDatBan = findViewById(R.id.recyclerViewDatBan);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        tvEmpty = findViewById(R.id.tvEmpty);
        
        // Setup SwipeRefreshLayout
        swipeRefresh.setColorSchemeResources(
            R.color.BluePrimary,
            R.color.RedPrimary,
            R.color.GreenPrimary
        );
        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                Log.d("QuanLyBanNguoiDung", "🔄 User triggered manual refresh");
                Log.d("QuanLyBanNguoiDung", "📡 Note: Real-time listener đã tự động update, manual refresh là optional");
                // Force refresh để clear cache và reconnect
                syncAndLoadDataForceRefresh();
            }
        });
    }

    private void initToolBar() {
        toolbar = findViewById(R.id.toolbarQuanLyBanNguoiDung);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Bàn của tôi");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
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
        Log.d("QuanLyBanNguoiDung", "onResume called");
        
        // ✅ REAL-TIME MODE: Không cần reload trong onResume
        // Firebase ValueEventListener đã tự động update UI khi data thay đổi
        // Listener đã được attach trong onCreate() và sẽ tiếp tục hoạt động
        Log.d("QuanLyBanNguoiDung", "📡 Real-time listener đã active - data sẽ tự động update");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Dừng Firebase listener khi activity bị destroy
        if (datBanDAO != null) {
            datBanDAO.stopFirebaseDirectListener();
        }
    }
}

