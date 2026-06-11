package app.edu.app.ui;

import androidx.annotation.NonNull;
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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import android.widget.AdapterView;
import android.widget.PopupMenu;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;

import app.edu.app.R;
import app.edu.app.adapter.SpinnerAdapterLoaiHang;
import app.edu.app.adapter.ThucUongAdapter;
import app.edu.app.dao.HangHoaDAO;
import app.edu.app.interfaces.ItemHangHoaOnClick;
import app.edu.app.model.HangHoa;
import app.edu.app.model.LoaiHang;
import app.edu.app.utils.MyToast;
import app.edu.app.utils.SyncUtils;

public class ThucUongActivity extends AppCompatActivity {
    private static final String TAG = "ThucUongActivity";
    public static final String MA_HANG_HOA = "maHangHoa";
    
    Toolbar toolbar;
    Spinner spFill;
    RecyclerView recyclerViewThucUong;
    
    // Firebase
    private DatabaseReference databaseReference;
    private DatabaseReference hangHoaRef;
    private DatabaseReference loaiHangRef;
    
    // Data lists
    private ArrayList<HangHoa> allHangHoaList = new ArrayList<>();
    private ArrayList<LoaiHang> allLoaiHangList = new ArrayList<>();
    
    private HangHoaDAO hangHoaDAO; // Still needed for delete operation

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_thuc_uong);
        initToolBar();
        initView();
        
        // Still needed for delete operation
        hangHoaDAO = new HangHoaDAO(this);
        
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        initFirebase();
        loadDataFromFirebase();

        spFill.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Skip if this is triggered during initial load
                if (isInitialLoad) {
                    Log.d(TAG, "Spinner selection during initial load - skipping");
                    return;
                }
                
                if (position == 0) {
                    // Tất cả
                    loadAllData();
                } else if (position == 1) {
                    // A - Z
                    loadDataSortedAtoZ();
                } else if (position == 2) {
                    // Z - A
                    loadDataSortedZtoA();
                } else {
                    // Filter by category
                    String selectedCategory = (String) spFill.getSelectedItem();
                    loadDataByCategory(selectedCategory);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }
    
    private void initFirebase() {
        databaseReference = FirebaseDatabase.getInstance().getReference();
        hangHoaRef = databaseReference.child("HangHoa");
        loaiHangRef = databaseReference.child("LoaiHang");
    }
    
    // Variables to track loading completion
    private boolean loaiHangLoaded = false;
    private boolean hangHoaLoaded = false;
    private boolean isInitialLoad = true; // Flag to prevent spinner trigger during initial load
    
    /**
     * Load data from Firebase Realtime Database
     */
    private void loadDataFromFirebase() {
        // Reset loading flags
        loaiHangLoaded = false;
        hangHoaLoaded = false;
        isInitialLoad = true; // Set flag to prevent spinner trigger
        
        // Load both LoaiHang and HangHoa
        loadLoaiHangFromFirebase();
        loadHangHoaFromFirebase();
    }
    
    /**
     * Check if both data sources are loaded and display data
     */
    private void checkAndDisplayData() {
        Log.d(TAG, "checkAndDisplayData - LoaiHang: " + loaiHangLoaded + ", HangHoa: " + hangHoaLoaded);
        if (loaiHangLoaded && hangHoaLoaded) {
            Log.d(TAG, "Both data sources loaded, displaying data");
            loadAllData();
            isInitialLoad = false; // Allow spinner to work normally now
        } else {
            Log.d(TAG, "Waiting for all data sources to load...");
        }
    }
    
    /**
     * Load LoaiHang from Firebase
     */
    private void loadLoaiHangFromFirebase() {
        Log.d(TAG, "Starting to load LoaiHang from Firebase...");
        loaiHangRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                try {
                    Log.d(TAG, "LoaiHang onDataChange triggered");
                    allLoaiHangList.clear();
                    
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        try {
                            Map<String, Object> map = (Map<String, Object>) snapshot.getValue();
                            if (map != null) {
                                LoaiHang loaiHang = SyncUtils.convertMapToLoaiHang(map);
                                allLoaiHangList.add(loaiHang);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing LoaiHang item: " + e.getMessage());
                        }
                    }
                    
                    Log.d(TAG, "Loaded " + allLoaiHangList.size() + " loại hàng from Firebase");
                    fillSpinner();
                } catch (Exception e) {
                    Log.e(TAG, "Error in loadLoaiHang onDataChange: " + e.getMessage());
                } finally {
                    loaiHangLoaded = true;
                    Log.d(TAG, "LoaiHang loaded flag set to true");
                    checkAndDisplayData();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Error loading LoaiHang: " + databaseError.getMessage());
                loaiHangLoaded = true; // Set flag even on error
                checkAndDisplayData();
                Toast.makeText(ThucUongActivity.this, "Lỗi tải loại món", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    /**
     * Load HangHoa from Firebase
     */
    private void loadHangHoaFromFirebase() {
        Log.d(TAG, "Starting to load HangHoa from Firebase...");
        hangHoaRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                try {
                    Log.d(TAG, "HangHoa onDataChange triggered");
                    allHangHoaList.clear();
                    
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        try {
                            Map<String, Object> map = (Map<String, Object>) snapshot.getValue();
                            if (map != null) {
                                HangHoa hangHoa = SyncUtils.convertMapToHangHoa(map);
                                allHangHoaList.add(hangHoa);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing HangHoa item: " + e.getMessage());
                        }
                    }
                    
                    Log.d(TAG, "Loaded " + allHangHoaList.size() + " hàng hóa from Firebase");
                } catch (Exception e) {
                    Log.e(TAG, "Error in loadHangHoa onDataChange: " + e.getMessage());
                } finally {
                    hangHoaLoaded = true;
                    Log.d(TAG, "HangHoa loaded flag set to true");
                    checkAndDisplayData();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Error loading HangHoa: " + databaseError.getMessage());
                hangHoaLoaded = true; // Set flag even on error
                checkAndDisplayData();
                Toast.makeText(ThucUongActivity.this, "Lỗi tải danh sách món", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showMenuPopup(View view, HangHoa hangHoa) {
        // Hiển thị menu (Cập nhật xoá)
        PopupMenu popup = new PopupMenu(ThucUongActivity.this, view);
        popup.getMenuInflater().inflate(R.menu.menu_more, popup.getMenu());

        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @SuppressLint("NonConstantResourceId")
            public boolean onMenuItemClick(MenuItem item) {
//                switch (item.getItemId()) {
//                    case R.id.menu_update:
//                        openCapNhatThucUongActivity(hangHoa);
//                        break;
//                    case R.id.menu_delete:
//                        deleteHangHoa(hangHoa);
//                        break;
//                }
                if (item.getItemId() == R.id.menu_update) {
                    openCapNhatThucUongActivity(hangHoa);
                } else if (item.getItemId() == R.id.menu_delete) {
                    deleteHangHoa(hangHoa);
                }
                return true;
            }
        });

        popup.show(); //showing
    }

    private void openCapNhatThucUongActivity(HangHoa hangHoa) {
        Intent intent = new Intent(ThucUongActivity.this, CapNhatHangHoaActivity.class);
        intent.putExtra(MA_HANG_HOA, String.valueOf(hangHoa.getMaHangHoa()));
        startActivity(intent);
        overridePendingTransition(R.anim.anim_in_right, R.anim.anim_out_left);
    }

    private void initView() {
        recyclerViewThucUong = findViewById(R.id.recyclerViewThucUong);
        spFill = findViewById(R.id.spinnerFill);
    }

    private void initToolBar() {
        toolbar = findViewById(R.id.toolbarThuUong);
        setSupportActionBar(toolbar);
    }

    /**
     * Load all data (no sorting/filtering)
     */
    private void loadAllData() {
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this, RecyclerView.VERTICAL, false);
        recyclerViewThucUong.setLayoutManager(linearLayoutManager);

        ThucUongAdapter thucUongAdapter = new ThucUongAdapter(new ArrayList<>(allHangHoaList), new ItemHangHoaOnClick() {
            @Override
            public void itemOclick(View view, HangHoa hangHoa) {
                showMenuPopup(view, hangHoa);
            }
        });
        recyclerViewThucUong.setAdapter(thucUongAdapter);
    }
    
    /**
     * Load data sorted A to Z
     */
    private void loadDataSortedAtoZ() {
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this, RecyclerView.VERTICAL, false);
        recyclerViewThucUong.setLayoutManager(linearLayoutManager);
        
        ArrayList<HangHoa> sortedList = new ArrayList<>(allHangHoaList);
        Collections.sort(sortedList, new Comparator<HangHoa>() {
            @Override
            public int compare(HangHoa h1, HangHoa h2) {
                return h1.getTenHangHoa().compareToIgnoreCase(h2.getTenHangHoa());
            }
        });
        
        ThucUongAdapter thucUongAdapter = new ThucUongAdapter(sortedList, new ItemHangHoaOnClick() {
            @Override
            public void itemOclick(View view, HangHoa hangHoa) {
                showMenuPopup(view, hangHoa);
            }
        });
        recyclerViewThucUong.setAdapter(thucUongAdapter);
    }
    
    /**
     * Load data sorted Z to A
     */
    private void loadDataSortedZtoA() {
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this, RecyclerView.VERTICAL, false);
        recyclerViewThucUong.setLayoutManager(linearLayoutManager);
        
        ArrayList<HangHoa> sortedList = new ArrayList<>(allHangHoaList);
        Collections.sort(sortedList, new Comparator<HangHoa>() {
            @Override
            public int compare(HangHoa h1, HangHoa h2) {
                return h2.getTenHangHoa().compareToIgnoreCase(h1.getTenHangHoa());
            }
        });
        
        ThucUongAdapter thucUongAdapter = new ThucUongAdapter(sortedList, new ItemHangHoaOnClick() {
            @Override
            public void itemOclick(View view, HangHoa hangHoa) {
                showMenuPopup(view, hangHoa);
            }
        });
        recyclerViewThucUong.setAdapter(thucUongAdapter);
    }
    
    /**
     * Load data filtered by category
     */
    private void loadDataByCategory(String categoryName) {
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this, RecyclerView.VERTICAL, false);
        recyclerViewThucUong.setLayoutManager(linearLayoutManager);
        
        // Find LoaiHang by name
        LoaiHang selectedLoaiHang = null;
        for (LoaiHang loaiHang : allLoaiHangList) {
            if (loaiHang.getTenLoai().equals(categoryName)) {
                selectedLoaiHang = loaiHang;
                break;
            }
        }
        
        if (selectedLoaiHang == null) {
            loadAllData();
            return;
        }
        
        // Filter by maLoai
        ArrayList<HangHoa> filteredList = new ArrayList<>();
        for (HangHoa hangHoa : allHangHoaList) {
            if (hangHoa.getMaLoai() == selectedLoaiHang.getMaLoai()) {
                filteredList.add(hangHoa);
            }
        }
        
        ThucUongAdapter thucUongAdapter = new ThucUongAdapter(filteredList, new ItemHangHoaOnClick() {
            @Override
            public void itemOclick(View view, HangHoa hangHoa) {
                showMenuPopup(view, hangHoa);
            }
        });
        recyclerViewThucUong.setAdapter(thucUongAdapter);
    }

    private void deleteHangHoa(HangHoa hangHoa) {
        AlertDialog.Builder builder = new AlertDialog.Builder(ThucUongActivity.this, R.style.AlertDialogTheme);
        builder.setMessage("Bạn có muốn xoá loại " + hangHoa.getTenHangHoa() + "?");
        builder.setPositiveButton("Xoá", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (hangHoaDAO.deleteHangHoa(String.valueOf(hangHoa.getMaHangHoa()))) {
                    // Xoá thức uống
                    MyToast.successful(ThucUongActivity.this, "Xoá thành công");
                    // Reload data from Firebase after deletion
                    loadDataFromFirebase();
                } else {
                    MyToast.error(ThucUongActivity.this, "Xoá không thành công, Có hoá đơn tồn tại mã thức uống này");
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

    private void fillSpinner() {
        ArrayList<String> list = new ArrayList<>();
        list.add("Tất cả");
        list.add("A - Z");
        list.add("Z - A");
        for (LoaiHang loaiHang : allLoaiHangList) {
            list.add(loaiHang.getTenLoai());
        }
        SpinnerAdapterLoaiHang adapterLoaiHang = new SpinnerAdapterLoaiHang(this, list);
        spFill.setAdapter(adapterLoaiHang);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_add, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menu_add) {
            startActivity(new Intent(ThucUongActivity.this, ThemHangHoaActivity.class));
            overridePendingTransition(R.anim.anim_in_right, R.anim.anim_out_left);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.anim_in_left, R.anim.anim_out_right);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload data from Firebase when returning to activity
        loadDataFromFirebase();
    }
}