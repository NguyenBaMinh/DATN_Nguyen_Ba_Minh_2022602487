package app.edu.app.ui;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Map;

import app.edu.app.R;
import app.edu.app.adapter.LoaiHangAdapter;
import app.edu.app.adapter.ThucUongOderThemAdapter;
import app.edu.app.dao.BanDAO;
import app.edu.app.dao.HoaDonChiTietDAO;
import app.edu.app.interfaces.ItemLoaiHangOnClick;
import app.edu.app.interfaces.ItemOderOnClick;
import app.edu.app.model.Ban;
import app.edu.app.model.HangHoa;
import app.edu.app.model.HoaDonChiTiet;
import app.edu.app.model.LoaiHang;
import app.edu.app.utils.MyToast;
import app.edu.app.utils.SyncUtils;

public class ThemMonActivity extends AppCompatActivity {
    private static final String TAG = "ThemMonActivity";

    Toolbar toolbar;
    RecyclerView recyclerViewThucUongOder, recyclerViewLoaiMon;
    TextView tvDanhSachMonTitle;
    HoaDonChiTietDAO hoaDonChiTietDAO;
    BanDAO banDAO;
    ThucUongOderThemAdapter thucUongAdapter;
    LoaiHangAdapter loaiHangAdapter;

    // Firebase
    private DatabaseReference databaseReference;
    private DatabaseReference hangHoaRef;
    private DatabaseReference loaiHangRef;

    // Data lists
    private ArrayList<HangHoa> allHangHoaList = new ArrayList<>();
    private ArrayList<LoaiHang> allLoaiHangList = new ArrayList<>();
    private ArrayList<HangHoa> filteredHangHoaList = new ArrayList<>();

    private int selectedLoaiHang = -1;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_oder_them_mon);

        initToolBar();
        initView();
        initFirebase();

        hoaDonChiTietDAO = new HoaDonChiTietDAO(this);
        banDAO = new BanDAO(this); // ✅ Thêm BanDAO

        showLoading();
        loadDataFromFirebase();
    }

    private void initFirebase() {
        databaseReference = FirebaseDatabase.getInstance().getReference();
        hangHoaRef = databaseReference.child("HangHoa");
        loaiHangRef = databaseReference.child("LoaiHang");
    }

    private void showLoading() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Đang tải dữ liệu...");
        progressDialog.setCancelable(false);
        progressDialog.show();
    }

    private void hideLoading() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    private void loadDataFromFirebase() {
        loadLoaiHangFromFirebase();
        loadHangHoaFromFirebase();
    }

    private void loadLoaiHangFromFirebase() {
        loaiHangRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                allLoaiHangList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    try {
                        Map<String, Object> map = (Map<String, Object>) snapshot.getValue();
                        if (map != null) {
                            LoaiHang loaiHang = SyncUtils.convertMapToLoaiHang(map);
                            allLoaiHangList.add(loaiHang);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing LoaiHang: " + e.getMessage());
                    }
                }
                Log.d(TAG, "Loaded " + allLoaiHangList.size() + " loại hàng from Firebase");
                setupLoaiHangRecyclerView();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Error loading LoaiHang: " + databaseError.getMessage());
                hideLoading();
                Toast.makeText(ThemMonActivity.this, "Lỗi tải loại món", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadHangHoaFromFirebase() {
        hangHoaRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                allHangHoaList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    try {
                        Map<String, Object> map = (Map<String, Object>) snapshot.getValue();
                        if (map != null) {
                            HangHoa hangHoa = SyncUtils.convertMapToHangHoa(map);
                            if (hangHoa.getTrangThai() == HangHoa.STATUS_STILL) {
                                allHangHoaList.add(hangHoa);
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing HangHoa: " + e.getMessage());
                    }
                }
                Log.d(TAG, "Loaded " + allHangHoaList.size() + " hàng hóa from Firebase");
                filteredHangHoaList = new ArrayList<>(allHangHoaList);
                setupHangHoaRecyclerView();
                hideLoading();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Error loading HangHoa: " + databaseError.getMessage());
                hideLoading();
                Toast.makeText(ThemMonActivity.this, "Lỗi tải danh sách món", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupLoaiHangRecyclerView() {
        LinearLayoutManager loaiMonLayoutManager = new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false);
        recyclerViewLoaiMon.setLayoutManager(loaiMonLayoutManager);

        ArrayList<LoaiHang> displayList = new ArrayList<>();
        LoaiHang tatCa = new LoaiHang();
        tatCa.setMaLoai(-1);
        tatCa.setTenLoai("Tất cả");
        displayList.add(tatCa);
        displayList.addAll(allLoaiHangList);

        loaiHangAdapter = new LoaiHangAdapter(displayList, new ItemLoaiHangOnClick() {
            @Override
            public void itemOclick(View view, LoaiHang loaiHang) {
                selectedLoaiHang = loaiHang.getMaLoai();
                if (loaiHang.getMaLoai() == -1) {
                    tvDanhSachMonTitle.setText("Tất cả món");
                } else {
                    tvDanhSachMonTitle.setText(loaiHang.getTenLoai());
                }
                filterHangHoaByLoai(selectedLoaiHang);
            }
        });
        recyclerViewLoaiMon.setAdapter(loaiHangAdapter);
        loaiHangAdapter.resetSelection();
    }

    private void setupHangHoaRecyclerView() {
        LinearLayoutManager hangHoaLayoutManager = new LinearLayoutManager(this, RecyclerView.VERTICAL, false);
        recyclerViewThucUongOder.setLayoutManager(hangHoaLayoutManager);

        thucUongAdapter = new ThucUongOderThemAdapter(filteredHangHoaList, new ItemOderOnClick() {
            @Override
            public void itemOclick(View view, HangHoa hangHoa) {
                addHangHoaToHoaDon(hangHoa);
            }
        });
        recyclerViewThucUongOder.setAdapter(thucUongAdapter);
    }

    /**
     * Thêm món vào hóa đơn và cập nhật trạng thái bàn = Đang sử dụng (1)
     */
    private void addHangHoaToHoaDon(HangHoa hangHoa) {
        Intent intent = getIntent();
        String maHoaDon = intent.getStringExtra(MangVeActivity.MA_HOA_DON);
        String maBan = intent.getStringExtra("maBan"); // ✅ Lấy maBan từ Intent

        HoaDonChiTiet hoaDonChiTiet = new HoaDonChiTiet();
        hoaDonChiTiet.setMaHoaDon(Integer.parseInt(maHoaDon));
        hoaDonChiTiet.setMaHangHoa(hangHoa.getMaHangHoa());
        hoaDonChiTiet.setSoLuong(1);
        hoaDonChiTiet.setGiaTien(hangHoa.getGiaTien() * hoaDonChiTiet.getSoLuong());

        Calendar calendar = Calendar.getInstance();
        hoaDonChiTiet.setNgayXuatHoaDon(calendar.getTime());

        if (hoaDonChiTietDAO.insertHoaDonChiTiet(hoaDonChiTiet)) {
            MyToast.successful(ThemMonActivity.this, "Thêm thành công " + hangHoa.getTenHangHoa());

            // ✅ Cập nhật trạng thái bàn = Đang sử dụng (CO_KHACH = 1)
            if (maBan != null && !maBan.isEmpty()) {
                banDAO.getByMaBanFromFirebaseDirect(maBan, new BanDAO.OnBanListener() {
                    @Override
                    public void onBanReceived(Ban ban) {
                        // Chỉ update nếu bàn chưa ở trạng thái đang sử dụng
                        if (ban.getTrangThai() != Ban.CO_KHACH) {
                            ban.setTrangThai(Ban.CO_KHACH); // = 1
                            banDAO.updateBan(ban);
                            Log.d(TAG, "✅ Bàn " + maBan + " → Đang sử dụng");
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.e(TAG, "Lỗi load bàn khi update trạng thái: " + e.getMessage());
                    }
                });
            }
        } else {
            MyToast.error(ThemMonActivity.this, "Thêm thất bại");
        }
    }

    private void filterHangHoaByLoai(int maLoai) {
        filteredHangHoaList.clear();

        if (maLoai == -1) {
            filteredHangHoaList.addAll(allHangHoaList);
            tvDanhSachMonTitle.setText("Tất cả món");
        } else {
            for (HangHoa hangHoa : allHangHoaList) {
                if (hangHoa.getMaLoai() == maLoai) {
                    filteredHangHoaList.add(hangHoa);
                }
            }
        }

        if (thucUongAdapter != null) {
            thucUongAdapter = new ThucUongOderThemAdapter(filteredHangHoaList, new ItemOderOnClick() {
                @Override
                public void itemOclick(View view, HangHoa hangHoa) {
                    addHangHoaToHoaDon(hangHoa);
                }
            });
            recyclerViewThucUongOder.setAdapter(thucUongAdapter);
        }
    }

    private void initToolBar() {
        toolbar = findViewById(R.id.toolbarThemMon);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(view -> {
            Intent intent = new Intent(ThemMonActivity.this, OderActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void initView() {
        recyclerViewLoaiMon = findViewById(R.id.recyclerViewLoaiMon);
        recyclerViewThucUongOder = findViewById(R.id.recyclerViewThucUongOder);
        tvDanhSachMonTitle = findViewById(R.id.tvDanhSachMonTitle);

        tvDanhSachMonTitle.setOnClickListener(v -> {
            selectedLoaiHang = -1;
            tvDanhSachMonTitle.setText("Tất cả món");
            if (loaiHangAdapter != null) loaiHangAdapter.resetSelection();
            filterHangHoaByLoai(-1);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        hideLoading();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(ThemMonActivity.this, OderActivity.class);
        startActivity(intent);
        finish();
    }
}