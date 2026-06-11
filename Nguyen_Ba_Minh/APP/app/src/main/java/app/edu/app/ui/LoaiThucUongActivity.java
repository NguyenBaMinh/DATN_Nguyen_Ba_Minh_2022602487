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
import android.widget.PopupMenu;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Map;

import app.edu.app.R;
import app.edu.app.adapter.LoaiHangAdapter;
import app.edu.app.dao.LoaiHangDAO;
import app.edu.app.interfaces.ItemLoaiHangOnClick;
import app.edu.app.model.LoaiHang;
import app.edu.app.utils.MyToast;
import app.edu.app.utils.SyncUtils;

public class LoaiThucUongActivity extends AppCompatActivity {
    private static final String TAG = "LoaiThucUongActivity";
    
    public static final String MA_LOAI_HANG = "maLoaiHang";
    RecyclerView recyclerViewLoai;
    Toolbar toolbar;
    LoaiHangDAO loaiHangDAO; // Still needed for delete operation
    
    // Firebase
    private DatabaseReference databaseReference;
    private DatabaseReference loaiHangRef;
    
    // Data
    private ArrayList<LoaiHang> allLoaiHangList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loai_thuc_uong);
        initToolBar();
        initView();

        loaiHangDAO = new LoaiHangDAO(this); // Still needed for delete operation
        
        initFirebase();
        loadDataFromFirebase();
    }
    
    private void initFirebase() {
        databaseReference = FirebaseDatabase.getInstance().getReference();
        loaiHangRef = databaseReference.child("LoaiHang");
    }

    private void initView() {
        recyclerViewLoai = findViewById(R.id.recyclerViewLoai);
    }

    private void initToolBar() {
        toolbar = findViewById(R.id.toolbar_loai);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
    }

    /**
     * Load data from Firebase Realtime Database
     */
    private void loadDataFromFirebase() {
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
                    
                    // Display data
                    displayData();
                } catch (Exception e) {
                    Log.e(TAG, "Error in loadDataFromFirebase onDataChange: " + e.getMessage());
                    Toast.makeText(LoaiThucUongActivity.this, "Lỗi xử lý dữ liệu", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Error loading LoaiHang: " + databaseError.getMessage());
                Toast.makeText(LoaiThucUongActivity.this, "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    /**
     * Display data in RecyclerView
     */
    private void displayData() {
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this, RecyclerView.VERTICAL, false);
        recyclerViewLoai.setLayoutManager(linearLayoutManager);
        
        LoaiHangAdapter loaiHangAdapter = new LoaiHangAdapter(new ArrayList<>(allLoaiHangList), new ItemLoaiHangOnClick() {
            @Override
            public void itemOclick(View view, LoaiHang loaiHang) {
                PopupMenu popup = new PopupMenu(LoaiThucUongActivity.this, view);
                popup.getMenuInflater().inflate(R.menu.menu_more, popup.getMenu());

                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @SuppressLint("NonConstantResourceId")
                    public boolean onMenuItemClick(MenuItem item) {
                        if (item.getItemId() == R.id.menu_update) {
                            updateLoaiHang(loaiHang);
                        } else if (item.getItemId() == R.id.menu_delete) {
                            deleteLoaiHang(loaiHang);
                        }
                        return true;
                    }
                });

                popup.show();
            }
        });
        recyclerViewLoai.setAdapter(loaiHangAdapter);
    }

    private void updateLoaiHang(LoaiHang loaiHang) {
        // Cập nhật loại hàng
        Intent intent = new Intent(LoaiThucUongActivity.this, SuaLoaiActivity.class);
        intent.putExtra(MA_LOAI_HANG, String.valueOf(loaiHang.getMaLoai()));
        startActivity(intent);
        overridePendingTransition(R.anim.anim_in_right, R.anim.anim_out_left);
    }

    private void deleteLoaiHang(LoaiHang loaiHang) {
        // Xoá loại hàng
        AlertDialog.Builder builder = new AlertDialog.Builder(LoaiThucUongActivity.this, R.style.AlertDialogTheme);
        builder.setMessage("Bạn có muốn xoá loại " + loaiHang.getTenLoai()+"?");

        builder.setPositiveButton("Xoá", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (loaiHangDAO.deleteLoaiHang(String.valueOf(loaiHang.getMaLoai()))) {
                    MyToast.successful(LoaiThucUongActivity.this, "Xoá thành công");
                    // Reload from Firebase after deletion
                    loadDataFromFirebase();
                } else {
                    MyToast.error(LoaiThucUongActivity.this, "Có thức uống thuộc mã loại này");
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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_add, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menu_add) {
            startActivity(new Intent(LoaiThucUongActivity.this, ThemLoaiActivity.class));
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