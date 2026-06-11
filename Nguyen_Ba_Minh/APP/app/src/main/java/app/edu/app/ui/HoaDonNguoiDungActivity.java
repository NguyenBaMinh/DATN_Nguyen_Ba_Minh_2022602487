package app.edu.app.ui;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import app.edu.app.R;
import app.edu.app.adapter.HoaDonAdapter;
import app.edu.app.interfaces.ItemHoaDonOnClick;
import app.edu.app.model.HoaDon;
import app.edu.app.model.HoaDonMangVe;
import app.edu.app.utils.SyncUtils;

public class HoaDonNguoiDungActivity extends AppCompatActivity {
    private static final String TAG = "HoaDonNguoiDungActivity";
    public static final String MA_HOA_DON = "maHoaDon";
    
    Toolbar toolbar;
    RecyclerView recyclerViewHoaDon;
    
    // Firebase
    private DatabaseReference databaseReference;
    private DatabaseReference hoaDonRef;
    private DatabaseReference hoaDonMangVeRef;
    
    // Data
    private List<HoaDon> hoaDonList = new ArrayList<>();
    private String maKhachHang;
    private boolean isLoadingFinished = false; // Flag to prevent multiple calls to finishLoadingData

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hoa_don_nguoi_dung);

        initToolBar();
        maKhachHang = getIntent().getStringExtra("maNguoiDung");
        
        // Log để debug
        Log.d(TAG, "onCreate - maKhachHang received: " + maKhachHang);
        if (maKhachHang == null || maKhachHang.isEmpty()) {
            Log.e(TAG, "maKhachHang is null or empty in onCreate!");
            // Try to get from SharedPreferences as fallback
            android.content.SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
            maKhachHang = sharedPreferences.getString("maNguoiDung", "");
            Log.d(TAG, "Tried to get maKhachHang from SharedPreferences: " + maKhachHang);
        }
        
        initView();
        initFirebase();
        
        isLoadingFinished = false; // Reset flag
        hoaDonList.clear(); // Clear old data
        loadDataFromFirebase();
    }
    
    private void initFirebase() {
        databaseReference = FirebaseDatabase.getInstance().getReference();
        hoaDonRef = databaseReference.child("HoaDon");
        hoaDonMangVeRef = databaseReference.child("HoaDonMangVe");
    }
    
    /**
     * Load data from Firebase Realtime Database
     */
    private void loadDataFromFirebase() {
        if (maKhachHang == null || maKhachHang.isEmpty()) {
            Log.e(TAG, "maKhachHang is null or empty");
            Toast.makeText(this, "Không tìm thấy thông tin khách hàng", Toast.LENGTH_SHORT).show();
            setupRecyclerView();
            return;
        }
        
        hoaDonList.clear();
        
        Log.d(TAG, "Starting to load HoaDon for maKhachHang: " + maKhachHang);
        
        // Load HoaDon by maKhachHang
        hoaDonRef.orderByChild("maKhachHang")
                .equalTo(maKhachHang)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (!dataSnapshot.exists()) {
                            Log.d(TAG, "No HoaDon found for user: " + maKhachHang);
                            // Try loading all and filter manually
                            loadAllHoaDonAndFilter();
                            return;
                        }
                        
                        int totalCount = 0;
                        int addedCount = 0;
                        
                        Log.d(TAG, "Found " + dataSnapshot.getChildrenCount() + " HoaDon records for user: " + maKhachHang);
                        
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            totalCount++;
                            try {
                                Map<String, Object> map = (Map<String, Object>) snapshot.getValue();
                                if (map != null) {
                                    HoaDon hoaDon = SyncUtils.convertMapToHoaDon(map);
                                    
                                    if (hoaDon == null || hoaDon.getMaHoaDon() == 0) {
                                        Log.w(TAG, "Invalid HoaDon parsed from snapshot: " + snapshot.getKey());
                                        continue;
                                    }
                                    
                                    // Log để debug
                                    Log.d(TAG, "HoaDon maHoaDon: " + hoaDon.getMaHoaDon() + 
                                          ", maKhachHang: " + hoaDon.getMaKhachHang() + 
                                          ", trangThai: " + hoaDon.getTrangThai());
                                    
                                    // Check if maKhachHang matches (Firebase query should handle this, but double check)
                                    boolean maKhachHangMatches = maKhachHang.equals(hoaDon.getMaKhachHang());
                                    
                                    // For now, add all invoices regardless of status to see what we get
                                    if (maKhachHangMatches) {
                                        hoaDonList.add(hoaDon);
                                        addedCount++;
                                        Log.d(TAG, "Added HoaDon " + hoaDon.getMaHoaDon() + " - trangThai: " + hoaDon.getTrangThai());
                                    } else {
                                        Log.d(TAG, "Skipped HoaDon " + hoaDon.getMaHoaDon() + 
                                              " - maKhachHang mismatch. Expected: " + maKhachHang + ", Got: " + hoaDon.getMaKhachHang());
                                    }
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing HoaDon from snapshot " + snapshot.getKey() + ": " + e.getMessage(), e);
                            }
                        }
                        
                        Log.d(TAG, "Loaded " + addedCount + "/" + totalCount + " HoaDon from Firebase for user: " + maKhachHang);
                        
                        // Load HoaDonMangVe by maKhachHang
                        loadHoaDonMangVeFromFirebase();
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.e(TAG, "Error loading HoaDon: " + databaseError.getMessage(), databaseError.toException());
                        Toast.makeText(HoaDonNguoiDungActivity.this, "Lỗi tải hóa đơn: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                        setupRecyclerView();
                    }
                });
    }
    
    /**
     * Load HoaDonMangVe from Firebase by maKhachHang
     */
    private void loadHoaDonMangVeFromFirebase() {
        hoaDonMangVeRef.orderByChild("maKhachHang")
                .equalTo(maKhachHang)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (!dataSnapshot.exists()) {
                            Log.d(TAG, "No HoaDonMangVe found for user: " + maKhachHang);
                            finishLoadingData();
                            return;
                        }
                        
                        int totalCount = 0;
                        int addedCount = 0;
                        
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            totalCount++;
                            try {
                                Map<String, Object> map = (Map<String, Object>) snapshot.getValue();
                                if (map != null) {
                                    HoaDonMangVe hoaDonMangVe = SyncUtils.convertMapToHoaDonMangVe(map);
                                    
                                    if (hoaDonMangVe == null || hoaDonMangVe.getMaHoaDon() == 0) {
                                        Log.w(TAG, "Invalid HoaDonMangVe parsed from snapshot: " + snapshot.getKey());
                                        continue;
                                    }
                                    
                                    // Log để debug
                                    Log.d(TAG, "HoaDonMangVe maHoaDon: " + hoaDonMangVe.getMaHoaDon() + 
                                          ", maKhachHang: " + hoaDonMangVe.getMaKhachHang() + 
                                          ", trangThai: " + hoaDonMangVe.getTrangThai());
                                    
                                    // Check if maKhachHang matches (Firebase query should handle this)
                                    boolean maKhachHangMatches = maKhachHang.equals(hoaDonMangVe.getMaKhachHang());
                                    
                                    // For now, add all invoices regardless of status
                                    if (maKhachHangMatches) {
                                        // Convert HoaDonMangVe to HoaDon
                                        HoaDon hoaDon = new HoaDon(
                                            hoaDonMangVe.getMaHoaDon(),
                                            hoaDonMangVe.getGioVao(),
                                            hoaDonMangVe.getGioRa(),
                                            HoaDon.DA_THANH_TOAN, // Map DA_DUYET to DA_THANH_TOAN for display
                                            hoaDonMangVe.getMaKhachHang(),
                                            -1  // maBan = -1 for mang ve
                                        );
                                        hoaDonList.add(hoaDon);
                                        addedCount++;
                                        Log.d(TAG, "Added HoaDonMangVe " + hoaDonMangVe.getMaHoaDon() + " - trangThai: " + hoaDonMangVe.getTrangThai());
                                    } else {
                                        Log.d(TAG, "Skipped HoaDonMangVe " + hoaDonMangVe.getMaHoaDon() + 
                                              " - maKhachHang mismatch. Expected: " + maKhachHang + ", Got: " + hoaDonMangVe.getMaKhachHang());
                                    }
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing HoaDonMangVe from snapshot " + snapshot.getKey() + ": " + e.getMessage(), e);
                            }
                        }
                        
                        Log.d(TAG, "Loaded " + addedCount + "/" + totalCount + " HoaDonMangVe from Firebase for user: " + maKhachHang);
                        
                        finishLoadingData();
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.e(TAG, "Error loading HoaDonMangVe: " + databaseError.getMessage(), databaseError.toException());
                        Toast.makeText(HoaDonNguoiDungActivity.this, "Lỗi tải hóa đơn mang về: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                        
                        // Still setup with what we have
                        finishLoadingData();
                    }
                });
    }
    
    /**
     * Finish loading data: sort, setup RecyclerView, hide loading
     */
    private void finishLoadingData() {
        Log.d(TAG, "finishLoadingData CALLED - isLoadingFinished: " + isLoadingFinished);
        
        // Prevent multiple calls
        if (isLoadingFinished) {
            Log.w(TAG, "finishLoadingData already called, ignoring");
            return;
        }
        
        // Ensure this runs on UI thread
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    isLoadingFinished = true;
                    
                    Log.d(TAG, "finishLoadingData - Running on UI thread. Total loaded: " + hoaDonList.size() + " invoices for user: " + maKhachHang);
                    
                    if (hoaDonList.isEmpty()) {
                        Log.d(TAG, "No invoices found for user: " + maKhachHang);
                        Toast.makeText(HoaDonNguoiDungActivity.this, "Không có hóa đơn nào", Toast.LENGTH_SHORT).show();
                    }
                    
                    // Sort by gioVao descending (newest first)
                    if (!hoaDonList.isEmpty()) {
                        Log.d(TAG, "finishLoadingData - Sorting invoice list");
                        sortHoaDonList();
                    }
                    
                    // Setup RecyclerView
                    Log.d(TAG, "finishLoadingData - Setting up RecyclerView");
                    setupRecyclerView();
                    
                    Log.d(TAG, "finishLoadingData - COMPLETED. RecyclerView should now show " + hoaDonList.size() + " invoices");
                } catch (Exception e) {
                    Log.e(TAG, "Error in finishLoadingData: " + e.getMessage(), e);
                }
            }
        });
    }
    
    /**
     * Sort hóa đơn list by gioVao descending (newest first)
     * Also remove duplicates based on maHoaDon
     */
    private void sortHoaDonList() {
        if (hoaDonList == null || hoaDonList.isEmpty()) {
            return;
        }
        
        // Remove duplicates based on maHoaDon
        List<HoaDon> uniqueList = new ArrayList<>();
        Set<Integer> seenMaHoaDon = new HashSet<>();
        
        for (HoaDon hoaDon : hoaDonList) {
            if (hoaDon != null && hoaDon.getMaHoaDon() != 0) {
                if (!seenMaHoaDon.contains(hoaDon.getMaHoaDon())) {
                    seenMaHoaDon.add(hoaDon.getMaHoaDon());
                    uniqueList.add(hoaDon);
                }
            }
        }
        
        // Clear and add unique items back
        hoaDonList.clear();
        hoaDonList.addAll(uniqueList);
        
        // Sort by gioVao descending (newest first)
        Collections.sort(hoaDonList, new Comparator<HoaDon>() {
            @Override
            public int compare(HoaDon h1, HoaDon h2) {
                // Sort by gioVao descending (newest first)
                if (h1 == null || h1.getGioVao() == null) return 1;
                if (h2 == null || h2.getGioVao() == null) return -1;
                return h2.getGioVao().compareTo(h1.getGioVao());
            }
        });
        
        Log.d(TAG, "Sorted and removed duplicates: " + hoaDonList.size() + " unique invoices");
    }
    
    /**
     * Setup RecyclerView with data
     */
    private void setupRecyclerView() {
        Log.d(TAG, "setupRecyclerView - Setting up RecyclerView with " + hoaDonList.size() + " invoices");
        
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this, RecyclerView.VERTICAL, false);
        recyclerViewHoaDon.setLayoutManager(linearLayoutManager);
        
        // Create a copy to avoid concurrent modification
        List<HoaDon> displayList = new ArrayList<>(hoaDonList);
        
        Log.d(TAG, "setupRecyclerView - Display list size: " + displayList.size());
        
        HoaDonAdapter adapter = new HoaDonAdapter(HoaDonNguoiDungActivity.this, displayList, new ItemHoaDonOnClick() {
            @Override
            public void itemOclick(View view, HoaDon hoaDon) {
                Intent intent = new Intent(HoaDonNguoiDungActivity.this, ChiTietHoaDonActivity.class);
                intent.putExtra(MA_HOA_DON, String.valueOf(hoaDon.getMaHoaDon()));
                intent.putExtra("maKhachHang", hoaDon.getMaKhachHang());
                Log.d(TAG, "maKhachHang: " + hoaDon.getMaKhachHang());
                intent.putExtra("maBan", hoaDon.getMaBan());
                Log.d(TAG, "maBan: " + hoaDon.getMaBan());
                startActivity(intent);
                overridePendingTransition(R.anim.anim_in_right, R.anim.anim_out_left);
            }
        });
        
        recyclerViewHoaDon.setAdapter(adapter);
        adapter.notifyDataSetChanged();
        
        Log.d(TAG, "setupRecyclerView - Adapter set and notified. Item count: " + adapter.getItemCount());
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
        // Reload data when returning to activity
        isLoadingFinished = false; // Reset flag
        hoaDonList.clear(); // Clear old data
        loadDataFromFirebase();
    }
    
    /**
     * Load all HoaDon and filter manually if Firebase query doesn't work
     */
    private void loadAllHoaDonAndFilter() {
        Log.d(TAG, "Loading all HoaDon and filtering manually for: " + maKhachHang);
        hoaDonRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) {
                    Log.d(TAG, "No HoaDon data in Firebase at all");
                    loadAllHoaDonMangVeAndFilter();
                    return;
                }
                
                int totalCount = 0;
                int addedCount = 0;
                
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    totalCount++;
                    try {
                        Map<String, Object> map = (Map<String, Object>) snapshot.getValue();
                        if (map != null) {
                            HoaDon hoaDon = SyncUtils.convertMapToHoaDon(map);
                            
                            if (hoaDon == null || hoaDon.getMaHoaDon() == 0) {
                                continue;
                            }
                            
                            // Filter by maKhachHang manually
                            if (maKhachHang.equals(hoaDon.getMaKhachHang())) {
                                hoaDonList.add(hoaDon);
                                addedCount++;
                                Log.d(TAG, "Manually added HoaDon " + hoaDon.getMaHoaDon() + " - trangThai: " + hoaDon.getTrangThai());
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing HoaDon: " + e.getMessage(), e);
                    }
                }
                
                Log.d(TAG, "Manually loaded " + addedCount + "/" + totalCount + " HoaDon for user: " + maKhachHang);
                loadAllHoaDonMangVeAndFilter();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Error loading all HoaDon: " + databaseError.getMessage(), databaseError.toException());
                loadAllHoaDonMangVeAndFilter();
            }
        });
    }
    
    /**
     * Load all HoaDonMangVe and filter manually
     */
    private void loadAllHoaDonMangVeAndFilter() {
        Log.d(TAG, "Loading all HoaDonMangVe and filtering manually for: " + maKhachHang);
        hoaDonMangVeRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) {
                    Log.d(TAG, "No HoaDonMangVe data in Firebase at all");
                    finishLoadingData();
                    return;
                }
                
                int totalCount = 0;
                int addedCount = 0;
                
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    totalCount++;
                    try {
                        Map<String, Object> map = (Map<String, Object>) snapshot.getValue();
                        if (map != null) {
                            HoaDonMangVe hoaDonMangVe = SyncUtils.convertMapToHoaDonMangVe(map);
                            
                            if (hoaDonMangVe == null || hoaDonMangVe.getMaHoaDon() == 0) {
                                continue;
                            }
                            
                            // Filter by maKhachHang manually
                            if (maKhachHang.equals(hoaDonMangVe.getMaKhachHang())) {
                                HoaDon hoaDon = new HoaDon(
                                    hoaDonMangVe.getMaHoaDon(),
                                    hoaDonMangVe.getGioVao(),
                                    hoaDonMangVe.getGioRa(),
                                    HoaDon.DA_THANH_TOAN,
                                    hoaDonMangVe.getMaKhachHang(),
                                    -1
                                );
                                hoaDonList.add(hoaDon);
                                addedCount++;
                                Log.d(TAG, "Manually added HoaDonMangVe " + hoaDonMangVe.getMaHoaDon() + " - trangThai: " + hoaDonMangVe.getTrangThai());
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing HoaDonMangVe: " + e.getMessage(), e);
                    }
                }
                
                Log.d(TAG, "Manually loaded " + addedCount + "/" + totalCount + " HoaDonMangVe for user: " + maKhachHang);
                finishLoadingData();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Error loading all HoaDonMangVe: " + databaseError.getMessage(), databaseError.toException());
                finishLoadingData();
            }
        });
    }
}
