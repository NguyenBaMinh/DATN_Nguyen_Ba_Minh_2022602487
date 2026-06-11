package app.edu.app.dao;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Map;

import app.edu.app.database.CoffeeDB;
import app.edu.app.interfaces.RealtimeUpdateCallback;
import app.edu.app.model.ThongBao;
import app.edu.app.utils.SyncUtils;
import app.edu.app.utils.XDate;

public class ThongBaoDAO extends FirebaseDAO {
    private static final String TABLE_NAME = "ThongBao";
    CoffeeDB coffeeDB;

    public ThongBaoDAO(Context context) {
        super(context);
        this.coffeeDB = new CoffeeDB(context);
    }

    // Add this at class level
    private RealtimeUpdateCallback thongBaoCallback;

    /**
     * Start real-time synchronization for ThongBao table
     * Optimized to process updates in background thread
     */
    public void startRealtimeSync() {
        thongBaoCallback = new RealtimeUpdateCallback() {
            @Override
            public void onDataChanged(DataSnapshot dataSnapshot) {
                // Extract data quickly from snapshot (lightweight operation)
                final ArrayList<Map<String, Object>> dataList = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    try {
                        Map<String, Object> map = (Map<String, Object>) snapshot.getValue();
                        if (map != null) {
                            dataList.add(map);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error extracting Firebase data in realtime sync", e);
                    }
                }
                
                // Process all data in background thread to avoid blocking UI
                runInBackground(() -> {
                    ArrayList<ThongBao> toInsert = new ArrayList<>();
                    ArrayList<ThongBao> toUpdate = new ArrayList<>();
                    
                    for (Map<String, Object> map : dataList) {
                        try {
                            ThongBao firebaseThongBao = SyncUtils.convertMapToThongBao(map);
                            
                            try {
                                ThongBao localThongBao = getByMaThongBao(String.valueOf(firebaseThongBao.getMaThongBao()));
                                // Check if update is needed
                                if (!localThongBao.getNoiDung().equals(firebaseThongBao.getNoiDung()) ||
                                        localThongBao.getTrangThai() != firebaseThongBao.getTrangThai()) {
                                    toUpdate.add(firebaseThongBao);
                                }
                            } catch (Exception e) {
                                // Item doesn't exist locally, insert it
                                toInsert.add(firebaseThongBao);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing Firebase ThongBao update", e);
                        }
                    }
                    
                    // Batch insert
                    for (ThongBao thongBao : toInsert) {
                        insertThongBaoFromFirebase(thongBao);
                    }
                    
                    // Batch update
                    for (ThongBao thongBao : toUpdate) {
                        updateThongBaoFromFirebase(thongBao);
                    }
                    
                    if (!toInsert.isEmpty() || !toUpdate.isEmpty()) {
                        Log.d(TAG, "Realtime sync: " + toInsert.size() + " inserted, " + toUpdate.size() + " updated");
                    }
                });
            }
        };

        setupRealtimeListener(TABLE_NAME, thongBaoCallback);
    }

    /**
     * Stop real-time synchronization
     */
    public void stopRealtimeSync() {
        removeRealtimeListener(TABLE_NAME);
    }

    // Firebase update methods
    private boolean updateThongBaoFromFirebase(ThongBao thongBao) {
        SQLiteDatabase sqLiteDatabase = coffeeDB.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("trangThai", thongBao.getTrangThai());
        values.put("noiDung", thongBao.getNoiDung());
        values.put("ngayThongBao", XDate.toStringDate(thongBao.getNgayThongBao()));
        values.put("maNguoiDung", thongBao.getMaNguoiDung()); // null = thông báo chung
        values.put("tieuDe", thongBao.getTieuDe() != null ? thongBao.getTieuDe() : "Thông báo");

        int check = sqLiteDatabase.update("THONGBAO", values, "maThongBao=?",
                new String[]{String.valueOf(thongBao.getMaThongBao())});

        if (check > 0) {
            Log.d(TAG, "Updated ThongBao from Firebase: " + thongBao.getMaThongBao());
        }

        return check > 0;
    }

    private boolean insertThongBaoFromFirebase(ThongBao thongBao) {
        SQLiteDatabase sqLiteDatabase = coffeeDB.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("maThongBao", thongBao.getMaThongBao());
        values.put("trangThai", thongBao.getTrangThai());
        values.put("noiDung", thongBao.getNoiDung());
        values.put("ngayThongBao", XDate.toStringDate(thongBao.getNgayThongBao()));
        values.put("maNguoiDung", thongBao.getMaNguoiDung()); // null = thông báo chung
        values.put("tieuDe", thongBao.getTieuDe() != null ? thongBao.getTieuDe() : "Thông báo");

        long check = sqLiteDatabase.insert("THONGBAO", null, values);

        if (check != -1) {
            Log.d(TAG, "Inserted ThongBao from Firebase: " + thongBao.getMaThongBao());
        }

        return check != -1;
    }

    @SuppressLint("Range")
    public ArrayList<ThongBao> get(String sql, String... choose) {
        ArrayList<ThongBao> list = new ArrayList<>();
        SQLiteDatabase sqLiteDatabase = coffeeDB.getReadableDatabase();
        @SuppressLint("Recycle") Cursor cursor = sqLiteDatabase.rawQuery(sql, choose);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            do {
                ThongBao thongBao = new ThongBao();
                thongBao.setMaThongBao(cursor.getInt(cursor.getColumnIndex("maThongBao")));
                thongBao.setNoiDung(cursor.getString(cursor.getColumnIndex("noiDung")));
                thongBao.setTrangThai(cursor.getInt(cursor.getColumnIndex("trangThai")));
                
                // Đọc maNguoiDung và tieuDe (có thể null)
                int maNguoiDungIndex = cursor.getColumnIndex("maNguoiDung");
                if (maNguoiDungIndex != -1) {
                    thongBao.setMaNguoiDung(cursor.getString(maNguoiDungIndex));
                }
                
                int tieuDeIndex = cursor.getColumnIndex("tieuDe");
                if (tieuDeIndex != -1) {
                    String tieuDe = cursor.getString(tieuDeIndex);
                    thongBao.setTieuDe(tieuDe != null ? tieuDe : "Thông báo");
                } else {
                    thongBao.setTieuDe("Thông báo");
                }
                
                try {
                    thongBao.setNgayThongBao(XDate.toDate(cursor.getString(cursor.getColumnIndex("ngayThongBao"))));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                list.add(thongBao);
                // Removed verbose logging to improve performance
            } while (cursor.moveToNext());
        }

        return list;
    }
    
    /**
     * Get notifications for a specific user (including general notifications)
     * @param maNguoiDung User ID to get notifications for
     * @return List of notifications for this user
     */
    public ArrayList<ThongBao> getByMaNguoiDung(String maNguoiDung) {
        // Get notifications where maNguoiDung matches OR maNguoiDung is NULL (general notifications)
        String sql = "SELECT * FROM THONGBAO WHERE (maNguoiDung = ? OR maNguoiDung IS NULL) ORDER BY maThongBao DESC";
        return get(sql, maNguoiDung);
    }

    /**
     * Get all notifications from local database only (non-blocking)
     * Use this for UI display to avoid blocking the main thread
     */
    public ArrayList<ThongBao> getAllLocal() {
        String sqlGetAll = "SELECT * FROM THONGBAO ORDER BY maThongBao DESC";
        return get(sqlGetAll);
    }

    /**
     * Get all notifications with automatic sync from Firebase
     * Note: This will return local data immediately and sync in background
     */
    public ArrayList<ThongBao> getAll() {
        // Return local data immediately
        ArrayList<ThongBao> list = getAllLocal();

        // Fetch updates from Firebase in background (non-blocking)
        if (isNetworkAvailable() && shouldSync(TABLE_NAME)) {
            fetchFromFirebaseAsync();
        }

        return list;
    }

    /**
     * Sync notifications from Firebase with callback
     * @param callback Called when sync is complete
     */
    public void syncFromFirebase(Runnable callback) {
        if (!isNetworkAvailable()) {
            if (callback != null) callback.run();
            return;
        }

        // Firebase listener callback runs on main thread, but we'll extract data quickly
        // and process in background
        dbRef.child(TABLE_NAME).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // Extract data quickly from snapshot (lightweight operation)
                final ArrayList<Map<String, Object>> dataList = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    try {
                        Map<String, Object> map = (Map<String, Object>) snapshot.getValue();
                        if (map != null) {
                            dataList.add(map);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error extracting Firebase data", e);
                    }
                }
                
                // Now process all data in background thread
                runInBackground(() -> {
                    ArrayList<ThongBao> toInsert = new ArrayList<>();
                    ArrayList<ThongBao> toUpdate = new ArrayList<>();
                    
                    for (Map<String, Object> map : dataList) {
                        try {
                            ThongBao firebaseThongBao = SyncUtils.convertMapToThongBao(map);
                            
                            // Check if notification exists locally (on background thread)
                            try {
                                ThongBao localThongBao = getByMaThongBao(String.valueOf(firebaseThongBao.getMaThongBao()));
                                
                                // If local version is different, mark for update
                                if (!localThongBao.getNoiDung().equals(firebaseThongBao.getNoiDung()) ||
                                        localThongBao.getTrangThai() != firebaseThongBao.getTrangThai()) {
                                    toUpdate.add(firebaseThongBao);
                                }
                            } catch (Exception e) {
                                // Item doesn't exist locally, mark for insert
                                toInsert.add(firebaseThongBao);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing Firebase data", e);
                        }
                    }
                    
                    // Batch insert
                    for (ThongBao thongBao : toInsert) {
                        insertThongBaoFromFirebase(thongBao);
                    }
                    
                    // Batch update
                    for (ThongBao thongBao : toUpdate) {
                        updateThongBaoFromFirebase(thongBao);
                    }
                    
                    Log.d(TAG, "Sync completed: " + toInsert.size() + " inserted, " + toUpdate.size() + " updated");
                    
                    // Execute callback on main thread
                    if (callback != null) {
                        runOnUiThread(callback);
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Firebase fetch cancelled", error.toException());
                if (callback != null) {
                    runOnUiThread(callback);
                }
            }
        });
    }

    // Fetch data from Firebase asynchronously in background thread
    private void fetchFromFirebaseAsync() {
        // Use syncFromFirebase without callback for async background sync
        syncFromFirebase(null);
    }

    // Fetch data from Firebase (kept for backward compatibility)
    private void fetchFromFirebase() {
        fetchFromFirebaseAsync();
    }

    public ArrayList<ThongBao> getByTrangThaiChuaXem() {
        String sqlGetAll = "SELECT * FROM THONGBAO WHERE trangThai=?";

        return get(sqlGetAll, String.valueOf(ThongBao.STATUS_CHUA_XEM));
    }
    
    // ═══════════════════════════════════════════════════════════
    // ✅ FIREBASE DIRECT MODE - Load trực tiếp từ Firebase
    // ═══════════════════════════════════════════════════════════
    
    /**
     * Callback interface cho Firebase Direct mode
     */
    public interface OnThongBaoListListener {
        void onListReceived(ArrayList<ThongBao> list);
        void onError(Exception e);
    }
    
    private ValueEventListener firebaseDirectListenerThongBao;
    private OnThongBaoListListener thongBaoListListener;
    
    /**
     * Load tất cả thông báo TRỰC TIẾP từ Firebase (không dùng SQLite)
     * Sắp xếp theo maThongBao DESC (mới nhất lên trước)
     * 
     * @param listener Callback nhận kết quả
     */
    public void getAllFromFirebaseDirect(OnThongBaoListListener listener) {
        Log.d(TAG, "╔══════════════════════════════════════╗");
        Log.d(TAG, "║  getAllFromFirebaseDirect            ║");
        Log.d(TAG, "╚══════════════════════════════════════╝");
        Log.d(TAG, "➤ Load TẤT CẢ thông báo từ Firebase");
        
        this.thongBaoListListener = listener;
        
        // Remove listener cũ nếu có
        if (firebaseDirectListenerThongBao != null) {
            dbRef.child(TABLE_NAME).removeEventListener(firebaseDirectListenerThongBao);
        }
        
        // Tạo listener mới
        firebaseDirectListenerThongBao = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.d(TAG, "▶ onDataChange được gọi!");
                Log.d(TAG, "Firebase trả về: " + dataSnapshot.getChildrenCount() + " thông báo");
                
                ArrayList<ThongBao> resultList = new ArrayList<>();
                int processedCount = 0;
                int successCount = 0;
                int errorCount = 0;
                
                // Parse dữ liệu từ Firebase
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    processedCount++;
                    try {
                        if (snapshot.getValue() == null) {
                            Log.w(TAG, "  [" + processedCount + "] Snapshot value is null");
                            continue;
                        }
                        
                        Map<String, Object> map = (Map<String, Object>) snapshot.getValue();
                        if (map != null) {
                            ThongBao thongBao = parseThongBaoFromMap(map);
                            if (thongBao != null) {
                                resultList.add(thongBao);
                                successCount++;
                                Log.d(TAG, "  [" + processedCount + "] ✓ " + thongBao.getNoiDung().substring(0, Math.min(50, thongBao.getNoiDung().length())) + "...");
                            } else {
                                errorCount++;
                                Log.w(TAG, "  [" + processedCount + "] Failed to parse");
                            }
                        }
                    } catch (Exception e) {
                        errorCount++;
                        Log.e(TAG, "  [" + processedCount + "] Error parsing: " + snapshot.getKey(), e);
                    }
                }
                
                // Sắp xếp theo maThongBao DESC (mới nhất lên trước)
                resultList.sort((tb1, tb2) -> Integer.compare(tb2.getMaThongBao(), tb1.getMaThongBao()));
                
                Log.d(TAG, "╔══════════════════════════════════════╗");
                Log.d(TAG, "║           KẾT QUẢ LOAD               ║");
                Log.d(TAG, "╚══════════════════════════════════════╝");
                Log.d(TAG, "Đã xử lý: " + processedCount + " items");
                Log.d(TAG, "Thành công: " + successCount + " items ✓");
                Log.d(TAG, "Lỗi: " + errorCount + " items ✗");
                Log.d(TAG, "Trả về danh sách: " + resultList.size() + " thông báo");
                
                // Remove listener TRƯỚC khi callback
                if (firebaseDirectListenerThongBao != null) {
                    Log.d(TAG, "▶ Remove Firebase listener");
                    dbRef.child(TABLE_NAME).removeEventListener(firebaseDirectListenerThongBao);
                    firebaseDirectListenerThongBao = null;
                }
                
                // Callback về main thread
                final ArrayList<ThongBao> finalList = resultList;
                runOnUiThread(() -> {
                    if (thongBaoListListener != null) {
                        thongBaoListListener.onListReceived(finalList);
                        Log.d(TAG, "✓ Callback đã được gọi");
                    } else {
                        Log.w(TAG, "⚠ Listener is NULL, không thể callback!");
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Firebase listener cancelled", error.toException());
                
                runOnUiThread(() -> {
                    if (thongBaoListListener != null) {
                        thongBaoListListener.onError(error.toException());
                    }
                });
                
                if (firebaseDirectListenerThongBao != null) {
                    dbRef.child(TABLE_NAME).removeEventListener(firebaseDirectListenerThongBao);
                    firebaseDirectListenerThongBao = null;
                }
            }
        };
        
        // Attach listener (lấy một lần)
        Log.d(TAG, "▶ Attach Firebase listener");
        dbRef.child(TABLE_NAME).addListenerForSingleValueEvent(firebaseDirectListenerThongBao);
    }
    
    /**
     * Parse ThongBao từ Map
     * ✅ Đảm bảo parse đầy đủ maNguoiDung và tieuDe để lọc theo user đúng
     */
    private ThongBao parseThongBaoFromMap(Map<String, Object> map) {
        try {
            ThongBao thongBao = new ThongBao();
            
            // Parse maThongBao
            Object maThongBaoObj = map.get("maThongBao");
            if (maThongBaoObj instanceof Long) {
                thongBao.setMaThongBao(((Long) maThongBaoObj).intValue());
            } else if (maThongBaoObj instanceof Integer) {
                thongBao.setMaThongBao((Integer) maThongBaoObj);
            }
            
            // Parse noiDung
            Object noiDungObj = map.get("noiDung");
            if (noiDungObj != null) {
                thongBao.setNoiDung(noiDungObj.toString());
            }
            
            // Parse trangThai
            Object trangThaiObj = map.get("trangThai");
            if (trangThaiObj instanceof Long) {
                thongBao.setTrangThai(((Long) trangThaiObj).intValue());
            } else if (trangThaiObj instanceof Integer) {
                thongBao.setTrangThai((Integer) trangThaiObj);
            }
            
            // ✅ Parse maNguoiDung (có thể null - thông báo chung)
            Object maNguoiDungObj = map.get("maNguoiDung");
            if (maNguoiDungObj != null) {
                thongBao.setMaNguoiDung(maNguoiDungObj.toString());
            } else {
                thongBao.setMaNguoiDung(null); // Thông báo chung
            }
            
            // ✅ Parse tieuDe (có thể null)
            Object tieuDeObj = map.get("tieuDe");
            if (tieuDeObj != null) {
                thongBao.setTieuDe(tieuDeObj.toString());
            } else {
                thongBao.setTieuDe("Thông báo"); // Default title
            }
            
            // Parse ngayThongBao
            Object ngayThongBaoObj = map.get("ngayThongBao");
            if (ngayThongBaoObj != null) {
                try {
                    thongBao.setNgayThongBao(XDate.toDate(ngayThongBaoObj.toString()));
                } catch (ParseException e) {
                    Log.e(TAG, "Error parsing ngayThongBao: " + ngayThongBaoObj, e);
                }
            }
            
            return thongBao;
        } catch (Exception e) {
            Log.e(TAG, "Error parsing ThongBao", e);
            return null;
        }
    }
    
    /**
     * Dừng Firebase Direct listener (gọi khi destroy)
     */
    public void stopFirebaseDirectListener() {
        if (firebaseDirectListenerThongBao != null) {
            dbRef.child(TABLE_NAME).removeEventListener(firebaseDirectListenerThongBao);
            firebaseDirectListenerThongBao = null;
            Log.d(TAG, "✓ Stopped Firebase Direct listener");
        }
    }

    public boolean insertThongBao(ThongBao thongBao) {
        SQLiteDatabase sqLiteDatabase = coffeeDB.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("trangThai", thongBao.getTrangThai());
        values.put("noiDung", thongBao.getNoiDung());
        values.put("ngayThongBao", XDate.toStringDate(thongBao.getNgayThongBao()));
        values.put("maNguoiDung", thongBao.getMaNguoiDung()); // null = thông báo chung
        values.put("tieuDe", thongBao.getTieuDe() != null ? thongBao.getTieuDe() : "Thông báo");
        long check = sqLiteDatabase.insert("THONGBAO", null, values);

        // If local insert successful and network available, sync to Firebase
        if (check != -1 && isNetworkAvailable()) {
            // Get the auto-generated ID and set it to the object
            if (thongBao.getMaThongBao() == 0) {
                String query = "SELECT last_insert_rowid() as lastId";
                Cursor cursor = sqLiteDatabase.rawQuery(query, null);
                if (cursor.moveToFirst()) {
                    @SuppressLint("Range") int lastId = cursor.getInt(cursor.getColumnIndex("lastId"));
                    thongBao.setMaThongBao(lastId);
                }
                cursor.close();
            }

            syncToFirebase(thongBao);
        }

        return check != -1;
    }

    public boolean updateThongBao(ThongBao thongBao) {
        SQLiteDatabase sqLiteDatabase = coffeeDB.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("trangThai", thongBao.getTrangThai());
        values.put("noiDung", thongBao.getNoiDung());
        values.put("ngayThongBao", XDate.toStringDate(thongBao.getNgayThongBao()));
        int check = sqLiteDatabase.update("THONGBAO", values, "maThongBao=?", new String[]{String.valueOf(thongBao.getMaThongBao())});

        // If local update successful and network available, sync to Firebase
        if (check > 0 && isNetworkAvailable()) {
            syncToFirebase(thongBao);
        }

        return check > 0;
    }

    public boolean deleteThongBao(String maThongBao) {
        SQLiteDatabase sqLiteDatabase = coffeeDB.getWritableDatabase();
        int check = sqLiteDatabase.delete("THONGBAO", "maThongBao=?", new String[]{maThongBao});

        // If local delete successful and network available, delete from Firebase
        if (check > 0 && isNetworkAvailable()) {
            deleteFromFirebase(maThongBao);
        }

        return check > 0;
    }

    // Add a method to get ThongBao by ID
    public ThongBao getByMaThongBao(String maThongBao) {
        String sqlGetByMaThongBao = "SELECT * FROM THONGBAO WHERE maThongBao=?";
        ArrayList<ThongBao> list = get(sqlGetByMaThongBao, maThongBao);

        return list.get(0);
    }

    // Sync a ThongBao object to Firebase
    private void syncToFirebase(ThongBao thongBao) {
        Map<String, Object> thongBaoMap = SyncUtils.convertThongBaoToMap(thongBao);

        dbRef.child(TABLE_NAME).child(String.valueOf(thongBao.getMaThongBao()))
                .setValue(thongBaoMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        logSuccessfulSync("Sync", String.valueOf(thongBao.getMaThongBao()));
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        handleSyncError("Sync", String.valueOf(thongBao.getMaThongBao()), e);
                    }
                });
    }

    // Delete a ThongBao from Firebase
    private void deleteFromFirebase(String maThongBao) {
        dbRef.child(TABLE_NAME).child(maThongBao)
                .removeValue()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        logSuccessfulSync("Delete", maThongBao);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        handleSyncError("Delete", maThongBao, e);
                    }
                });
    }
}