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

public class HoaDonActivity extends AppCompatActivity {
    private static final String TAG = "HoaDonActivity";
    public static final String MA_HOA_DON = "maHoaDon";
    
    Toolbar toolbar;
    RecyclerView recyclerViewHoaDon;
    
    // Firebase
    private DatabaseReference databaseReference;
    private DatabaseReference hoaDonRef;
    private DatabaseReference hoaDonMangVeRef;
    
    // Data
    private List<HoaDon> hoaDonList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hoa_don);
        
        initToolBar();
        initView();
        initFirebase();
        
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
        hoaDonList.clear();
        
        // Load HoaDon (đã thanh toán)
        hoaDonRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) {
                    Log.d(TAG, "No HoaDon data in Firebase");
                    loadHoaDonMangVeFromFirebase();
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
                                Log.w(TAG, "Invalid HoaDon parsed from snapshot: " + snapshot.getKey());
                                continue;
                            }
                            
                            // Log trạng thái để debug
                            Log.d(TAG, "HoaDon maHoaDon: " + hoaDon.getMaHoaDon() + ", trangThai: " + hoaDon.getTrangThai() + " (expected: " + HoaDon.DA_THANH_TOAN + ")");
                            
                            // Chỉ lấy hóa đơn đã thanh toán
                            if (hoaDon.getTrangThai() == HoaDon.DA_THANH_TOAN) {
                                hoaDonList.add(hoaDon);
                                addedCount++;
                            } else {
                                Log.d(TAG, "Skipped HoaDon " + hoaDon.getMaHoaDon() + " - trangThai: " + hoaDon.getTrangThai());
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing HoaDon from snapshot " + snapshot.getKey() + ": " + e.getMessage(), e);
                    }
                }
                
                Log.d(TAG, "Loaded " + addedCount + "/" + totalCount + " HoaDon from Firebase (đã thanh toán)");
                
                // Load HoaDonMangVe (đã duyệt)
                loadHoaDonMangVeFromFirebase();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Error loading HoaDon: " + databaseError.getMessage(), databaseError.toException());
                Toast.makeText(HoaDonActivity.this, "Lỗi tải hóa đơn: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                // Still try to setup with empty list
                setupRecyclerView();
            }
        });
    }
    
    /**
     * Load HoaDonMangVe from Firebase
     */
    private void loadHoaDonMangVeFromFirebase() {
        hoaDonMangVeRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) {
                    Log.d(TAG, "No HoaDonMangVe data in Firebase");
                    // Continue to setup with what we have
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
                            
                            // Log trạng thái để debug
                            Log.d(TAG, "HoaDonMangVe maHoaDon: " + hoaDonMangVe.getMaHoaDon() + ", trangThai: " + hoaDonMangVe.getTrangThai() + " (expected: " + HoaDonMangVe.DA_DUYET + ")");
                            
                            // Chỉ lấy hóa đơn đã duyệt
                            if (hoaDonMangVe.getTrangThai() == HoaDonMangVe.DA_DUYET) {
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
                            } else {
                                Log.d(TAG, "Skipped HoaDonMangVe " + hoaDonMangVe.getMaHoaDon() + " - trangThai: " + hoaDonMangVe.getTrangThai());
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing HoaDonMangVe from snapshot " + snapshot.getKey() + ": " + e.getMessage(), e);
                    }
                }
                
                Log.d(TAG, "Loaded " + addedCount + "/" + totalCount + " HoaDonMangVe from Firebase (đã duyệt)");
                
                finishLoadingData();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Error loading HoaDonMangVe: " + databaseError.getMessage(), databaseError.toException());
                Toast.makeText(HoaDonActivity.this, "Lỗi tải hóa đơn mang về: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                
                // Still setup with what we have
                finishLoadingData();
            }
        });
    }
    
    /**
     * Finish loading data: sort, setup RecyclerView, hide loading
     */
    private void finishLoadingData() {
        Log.d(TAG, "Total loaded: " + hoaDonList.size() + " invoices");
        
        if (hoaDonList.isEmpty()) {
            Log.d(TAG, "No invoices found");
            Toast.makeText(this, "Không có hóa đơn nào", Toast.LENGTH_SHORT).show();
        }
        
        // Sort by gioVao descending (newest first)
        if (!hoaDonList.isEmpty()) {
            sortHoaDonList();
        }
        
        // Setup RecyclerView
        setupRecyclerView();
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
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this, RecyclerView.VERTICAL, false);
        recyclerViewHoaDon.setLayoutManager(linearLayoutManager);
        
        // Create a copy to avoid concurrent modification
        List<HoaDon> displayList = new ArrayList<>(hoaDonList);
        
        HoaDonAdapter adapter = new HoaDonAdapter(HoaDonActivity.this, displayList, new ItemHoaDonOnClick() {
            @Override
            public void itemOclick(View view, HoaDon hoaDon) {
                Intent intent = new Intent(HoaDonActivity.this, ChiTietHoaDonActivity.class);
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
        loadDataFromFirebase();
    }
}
