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

import java.util.ArrayList;
import java.util.Map;

import app.edu.app.database.CoffeeDB;
import app.edu.app.interfaces.RealtimeUpdateCallback;
import app.edu.app.model.DatBan;
import app.edu.app.model.NguoiDung;
import app.edu.app.utils.SyncUtils;

public class DatBanDAO extends FirebaseDAO {
    private static final String TABLE_NAME = "DatBan";
    private static final String TAG = "DatBanDAO";
    CoffeeDB coffeeDB;

    public DatBanDAO(Context context) {
        super(context);
        this.coffeeDB = new CoffeeDB(context);
        
        // Enable keepSynced để Firebase tự động sync node DatBan
        // Điều này đảm bảo dữ liệu luôn được cập nhật từ server
        try {
            dbRef.child(TABLE_NAME).keepSynced(true);
            Log.d(TAG, "✓ Enabled keepSynced for DatBan node");
        } catch (Exception e) {
            Log.e(TAG, "Failed to enable keepSynced", e);
        }
    }

    // Realtime update callback
    private RealtimeUpdateCallback datBanCallback;

    /**
     * Start real-time synchronization for DatBan table
     */
    public void startRealtimeSync() {
        datBanCallback = new RealtimeUpdateCallback() {
            @Override
            public void onDataChanged(DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    try {
                        Map<String, Object> map = (Map<String, Object>) snapshot.getValue();
                        if (map != null) {
                            DatBan firebaseDatBan = SyncUtils.convertMapToDatBan(map);
                            try {
                                DatBan localDatBan = getByMaDatBan(String.valueOf(firebaseDatBan.getMaDatBan()));
                                // Update if needed
                                updateDatBanFromFirebase(firebaseDatBan);
                            } catch (Exception e) {
                                // Item doesn't exist locally, insert it
                                insertDatBanFromFirebase(firebaseDatBan);
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing Firebase DatBan update", e);
                    }
                }
            }
        };

        setupRealtimeListener(TABLE_NAME, datBanCallback);
    }

    /**
     * Stop real-time synchronization
     */
    public void stopRealtimeSync() {
        removeRealtimeListener(TABLE_NAME);
    }

    // Firebase update methods
    private boolean updateDatBanFromFirebase(DatBan datBan) {
        SQLiteDatabase sqLiteDatabase = coffeeDB.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("maBan", datBan.getMaBan());
        values.put("maKhachHang", datBan.getMaKhachHang());
        values.put("ngayGioDat", datBan.getNgayGioDat());
        values.put("ngayGioSuDung", datBan.getNgayGioSuDung());
        values.put("trangThai", datBan.getTrangThai());
        values.put("ghiChu", datBan.getGhiChu());

        int check = sqLiteDatabase.update("DATBAN", values, "maDatBan=?",
                new String[]{String.valueOf(datBan.getMaDatBan())});

        if (check > 0) {
            Log.d(TAG, "Updated DatBan from Firebase: " + datBan.getMaDatBan());
        }

        return check > 0;
    }

    private boolean insertDatBanFromFirebase(DatBan datBan) {
        SQLiteDatabase sqLiteDatabase = coffeeDB.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("maDatBan", datBan.getMaDatBan());
        values.put("maBan", datBan.getMaBan());
        values.put("maKhachHang", datBan.getMaKhachHang());
        values.put("ngayGioDat", datBan.getNgayGioDat());
        values.put("ngayGioSuDung", datBan.getNgayGioSuDung());
        values.put("trangThai", datBan.getTrangThai());
        values.put("ghiChu", datBan.getGhiChu() != null ? datBan.getGhiChu() : "");

        long check = sqLiteDatabase.insert("DATBAN", null, values);

        if (check != -1) {
            Log.d(TAG, "Inserted DatBan from Firebase: " + datBan.getMaDatBan());
        }

        return check != -1;
    }

    @SuppressLint("Range")
    public ArrayList<DatBan> get(String sql, String... choose) {
        ArrayList<DatBan> list = new ArrayList<>();
        SQLiteDatabase sqLiteDatabase = coffeeDB.getReadableDatabase();
        @SuppressLint("Recycle") Cursor cursor = sqLiteDatabase.rawQuery(sql, choose);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            do {
                DatBan datBan = new DatBan();
                datBan.setMaDatBan(cursor.getInt(cursor.getColumnIndex("maDatBan")));
                datBan.setMaBan(cursor.getInt(cursor.getColumnIndex("maBan")));
                datBan.setMaKhachHang(cursor.getString(cursor.getColumnIndex("maKhachHang")));
                datBan.setNgayGioDat(cursor.getString(cursor.getColumnIndex("ngayGioDat")));
                datBan.setNgayGioSuDung(cursor.getString(cursor.getColumnIndex("ngayGioSuDung")));
                datBan.setTrangThai(cursor.getInt(cursor.getColumnIndex("trangThai")));
                datBan.setGhiChu(cursor.getString(cursor.getColumnIndex("ghiChu")));
                list.add(datBan);
                // Removed verbose logging to improve performance
            } while (cursor.moveToNext());
        }

        return list;
    }

    public ArrayList<DatBan> getAll() {
        String sqlGetAll = "SELECT * FROM DATBAN ORDER BY ngayGioDat DESC";
        ArrayList<DatBan> list = get(sqlGetAll);

        // If network is available, fetch updates from Firebase
        if (isNetworkAvailable()) {
            fetchFromFirebase();
        }

        return list;
    }

    // Fetch data from Firebase
    private void fetchFromFirebase() {
        dbRef.child(TABLE_NAME).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    try {
                        Map<String, Object> map = (Map<String, Object>) snapshot.getValue();
                        if (map != null) {
                            DatBan firebaseDatBan = SyncUtils.convertMapToDatBan(map);

                            try {
                                DatBan localDatBan = getByMaDatBan(String.valueOf(firebaseDatBan.getMaDatBan()));
                                // Update if needed
                                if (!localDatBan.getNgayGioSuDung().equals(firebaseDatBan.getNgayGioSuDung()) ||
                                    localDatBan.getTrangThai() != firebaseDatBan.getTrangThai()) {
                                    updateDatBan(firebaseDatBan);
                                }
                            } catch (Exception e) {
                                // Item doesn't exist locally, insert it
                                insertDatBan(firebaseDatBan);
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing Firebase data", e);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Firebase fetch cancelled", error.toException());
            }
        });
    }

    /**
     * Lấy danh sách đặt bàn chờ duyệt
     */
    public ArrayList<DatBan> getByTrangThaiChoDuyet() {
        String sql = "SELECT * FROM DATBAN WHERE trangThai=? ORDER BY ngayGioDat DESC";
        return get(sql, String.valueOf(DatBan.TRANG_THAI_CHO_DUYET));
    }

    /**
     * Lấy danh sách đặt bàn theo khách hàng
     */
    public ArrayList<DatBan> getByMaKhachHang(String maKhachHang) {
        String sql = "SELECT * FROM DATBAN WHERE maKhachHang=? ORDER BY ngayGioDat DESC";
        return get(sql, maKhachHang);
    }

    /**
     * Lấy đặt bàn đã được duyệt của khách hàng (để kiểm tra có thể đặt bàn không)
     */
    public ArrayList<DatBan> getByMaKhachHangDaDuyet(String maKhachHang) {
        String sql = "SELECT * FROM DATBAN WHERE maKhachHang=? AND trangThai=? ORDER BY ngayGioSuDung DESC";
        return get(sql, maKhachHang, String.valueOf(DatBan.TRANG_THAI_DA_DUYET));
    }

    /**
     * Lấy danh sách đặt bàn đã được duyệt hoặc chờ duyệt theo ngày/giờ sử dụng
     * Dùng để kiểm tra bàn nào đã được đặt trong thời gian đó
     * @param ngayGioSuDung Format: "dd-MM-yyyy HH:mm:ss"
     */
    public ArrayList<DatBan> getDaDuyetByNgayGioSuDung(String ngayGioSuDung) {
        // Filter theo cùng ngày (một ngày chỉ có thể đặt một lần)
        ArrayList<DatBan> result = new ArrayList<>();
        
        try {
            if (ngayGioSuDung == null || ngayGioSuDung.length() < 10) {
                Log.d(TAG, "Invalid date format: " + ngayGioSuDung);
                return result; // Return empty nếu format không đúng
            }
            
            // Lấy phần ngày (dd-MM-yyyy) từ ngày giờ đã chọn
            String targetDate = ngayGioSuDung.substring(0, 10);
            Log.d(TAG, "Filtering reservations for date: " + targetDate);
            
            // Lấy tất cả đặt bàn đã duyệt và chờ duyệt (chỉ những trạng thái có thể đặt được)
            ArrayList<DatBan> allDaDuyet = new ArrayList<>();
            allDaDuyet.addAll(getByTrangThaiChoDuyet());
            allDaDuyet.addAll(getByTrangThai(String.valueOf(DatBan.TRANG_THAI_DA_DUYET)));
            
            Log.d(TAG, "Total reservations to check: " + allDaDuyet.size());
            
            // Filter theo cùng ngày
            for (DatBan datBan : allDaDuyet) {
                if (datBan.getNgayGioSuDung() != null && datBan.getNgayGioSuDung().length() >= 10) {
                    String bookedDate = datBan.getNgayGioSuDung().substring(0, 10);
                    // So sánh chính xác phần ngày
                    if (bookedDate.equals(targetDate)) {
                        result.add(datBan);
                        Log.d(TAG, "Found booked table: " + datBan.getMaBan() + 
                              " on date: " + bookedDate + 
                              " (status: " + datBan.getTrangThai() + ")");
                    } else {
                        Log.d(TAG, "Skipping table " + datBan.getMaBan() + 
                              " - booked date: " + bookedDate + 
                              " != target date: " + targetDate);
                    }
                } else {
                    Log.d(TAG, "Invalid ngayGioSuDung format for table " + datBan.getMaBan() + 
                          ": " + datBan.getNgayGioSuDung());
                }
            }
            
            Log.d(TAG, "Total reservations found for date " + targetDate + ": " + result.size());
        } catch (Exception e) {
            Log.e(TAG, "Error filtering by date/time", e);
        }
        
        return result;
    }
    
    /**
     * Lấy danh sách đặt bàn theo trạng thái
     */
    private ArrayList<DatBan> getByTrangThai(String trangThai) {
        String sql = "SELECT * FROM DATBAN WHERE trangThai=? ORDER BY ngayGioSuDung DESC";
        return get(sql, trangThai);
    }

    /**
     * Kiểm tra bàn có được đặt trong ngày/giờ cụ thể không
     * @param maBan Mã bàn cần kiểm tra
     * @param ngayGioSuDung Format: "dd-MM-yyyy HH:mm:ss"
     * @return true nếu bàn đã được đặt (đã duyệt), false nếu còn trống
     */
    public boolean isBanDaDat(int maBan, String ngayGioSuDung) {
        ArrayList<DatBan> datBanList = getDaDuyetByNgayGioSuDung(ngayGioSuDung);
        for (DatBan datBan : datBanList) {
            if (datBan.getMaBan() == maBan) {
                return true;
            }
        }
        return false;
    }

    public boolean insertDatBan(DatBan datBan) {
        SQLiteDatabase sqLiteDatabase = coffeeDB.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("maBan", datBan.getMaBan());
        values.put("maKhachHang", datBan.getMaKhachHang());
        values.put("ngayGioDat", datBan.getNgayGioDat());
        values.put("ngayGioSuDung", datBan.getNgayGioSuDung());
        values.put("trangThai", datBan.getTrangThai());
        values.put("ghiChu", datBan.getGhiChu() != null ? datBan.getGhiChu() : "");
        
        long check = sqLiteDatabase.insert("DATBAN", null, values);

        // If local insert successful and network available, sync to Firebase
        if (check != -1 && isNetworkAvailable()) {
            // Get the auto-generated ID and set it to the object
            if (datBan.getMaDatBan() == 0) {
                String query = "SELECT last_insert_rowid() as lastId";
                Cursor cursor = sqLiteDatabase.rawQuery(query, null);
                if (cursor.moveToFirst()) {
                    @SuppressLint("Range") int lastId = cursor.getInt(cursor.getColumnIndex("lastId"));
                    datBan.setMaDatBan(lastId);
                }
                cursor.close();
            }
            
            datBan.setLastModified(System.currentTimeMillis());
            syncToFirebase(datBan);
        }

        return check != -1;
    }

    public boolean updateDatBan(DatBan datBan) {
        SQLiteDatabase sqLiteDatabase = coffeeDB.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("maBan", datBan.getMaBan());
        values.put("maKhachHang", datBan.getMaKhachHang());
        values.put("ngayGioDat", datBan.getNgayGioDat());
        values.put("ngayGioSuDung", datBan.getNgayGioSuDung());
        values.put("trangThai", datBan.getTrangThai());
        values.put("ghiChu", datBan.getGhiChu() != null ? datBan.getGhiChu() : "");
        
        int check = sqLiteDatabase.update("DATBAN", values, "maDatBan=?",
                new String[]{String.valueOf(datBan.getMaDatBan())});

        // If local update successful and network available, sync to Firebase
        if (check > 0 && isNetworkAvailable()) {
            datBan.setLastModified(System.currentTimeMillis());
            syncToFirebase(datBan);
        }

        return check > 0;
    }

    public boolean deleteDatBan(String maDatBan) {
        SQLiteDatabase sqLiteDatabase = coffeeDB.getWritableDatabase();
        int check = sqLiteDatabase.delete("DATBAN", "maDatBan=?", new String[]{maDatBan});

        // If local delete successful and network available, delete from Firebase
        if (check > 0 && isNetworkAvailable()) {
            deleteFromFirebase(maDatBan);
        }

        return check > 0;
    }

    // Add a method to get DatBan by ID
    public DatBan getByMaDatBan(String maDatBan) {
        String sqlGetByMaDatBan = "SELECT * FROM DATBAN WHERE maDatBan=?";
        ArrayList<DatBan> list = get(sqlGetByMaDatBan, maDatBan);
        
        if (list.isEmpty()) {
            throw new RuntimeException("DatBan not found with maDatBan: " + maDatBan);
        }

        return list.get(0);
    }

    // Sync a DatBan object to Firebase
    private void syncToFirebase(DatBan datBan) {
        Map<String, Object> datBanMap = SyncUtils.convertDatBanToMap(datBan);
        
        // Thêm tenBan và tenKhachHang theo cấu trúc JSON
        datBanMap.put("tenBan", "Bàn " + datBan.getMaBan());
        
        // Lấy tên khách hàng từ NguoiDung
        try {
            NguoiDungDAO nguoiDungDAO = new NguoiDungDAO(context);
            NguoiDung nguoiDung = nguoiDungDAO.getByMaNguoiDung(datBan.getMaKhachHang());
            if (nguoiDung != null) {
                datBanMap.put("tenKhachHang", nguoiDung.getHoVaTen());
            } else {
                datBanMap.put("tenKhachHang", datBan.getMaKhachHang());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting user name for DatBan", e);
            datBanMap.put("tenKhachHang", datBan.getMaKhachHang());
        }
        
        // Thêm id field (giống như trong JSON)
        datBanMap.put("id", datBan.getMaDatBan());

        dbRef.child(TABLE_NAME).child(String.valueOf(datBan.getMaDatBan()))
                .setValue(datBanMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        logSuccessfulSync("Sync", String.valueOf(datBan.getMaDatBan()));
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        handleSyncError("Sync", String.valueOf(datBan.getMaDatBan()), e);
                    }
                });
    }

    // Delete a DatBan from Firebase
    private void deleteFromFirebase(String maDatBan) {
        dbRef.child(TABLE_NAME).child(maDatBan)
                .removeValue()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        logSuccessfulSync("Delete", maDatBan);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        handleSyncError("Delete", maDatBan, e);
                    }
                });
    }

    /**
     * Interface để nhận callback khi lấy danh sách đặt bàn từ Firebase
     */
    public interface OnDatBanListListener {
        void onListReceived(ArrayList<DatBan> list);
        void onError(Exception e);
    }

    private ValueEventListener firebaseDirectListenerDatBan;
    private OnDatBanListListener datBanListListener;

    /**
     * Lấy danh sách đặt bàn đã duyệt (hoặc tất cả) theo mã khách hàng TRỰC TIẾP từ Firebase
     * (không sync về SQLite)
     * 
     * REAL-TIME MODE: Listener sẽ tự động trigger khi dữ liệu thay đổi trên Firebase
     * 
     * @param maKhachHang Mã khách hàng
     * @param listener Callback để nhận kết quả (sẽ được gọi lại mỗi khi data thay đổi)
     */
    public void getByMaKhachHangDaDuyetFromFirebaseDirect(String maKhachHang, OnDatBanListListener listener) {
        getByMaKhachHangDaDuyetFromFirebaseDirect(maKhachHang, listener, false);
    }
    
    /**
     * Lấy danh sách đặt bàn với option force reload từ server
     * 
     * @param maKhachHang Mã khách hàng
     * @param listener Callback để nhận kết quả
     * @param forceFromServer True để force load từ server (bỏ qua cache)
     */
    public void getByMaKhachHangDaDuyetFromFirebaseDirect(String maKhachHang, OnDatBanListListener listener, boolean forceFromServer) {
        this.datBanListListener = listener;
        
        Log.d(TAG, "getByMaKhachHangDaDuyetFromFirebaseDirect - forceFromServer: " + forceFromServer);
        
        // Remove listener cũ nếu có
        if (firebaseDirectListenerDatBan != null) {
            dbRef.child(TABLE_NAME).removeEventListener(firebaseDirectListenerDatBan);
        }
        
        // Tạo listener để lấy một lần
        firebaseDirectListenerDatBan = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                ArrayList<DatBan> resultList = new ArrayList<>();
                
                Log.d(TAG, "========================================");
                Log.d(TAG, "FIREBASE CALLBACK - Tìm đặt bàn đã duyệt");
                Log.d(TAG, "maKhachHang: [" + maKhachHang + "]");
                Log.d(TAG, "Tổng children trong Firebase: " + dataSnapshot.getChildrenCount());
                Log.d(TAG, "dataSnapshot.exists(): " + dataSnapshot.exists());
                Log.d(TAG, "dataSnapshot.getKey(): " + dataSnapshot.getKey());
                Log.d(TAG, "========================================");
                
                int processedCount = 0;
                int matchCount = 0;
                int nullCount = 0;
                
                // Xử lý dữ liệu từ Firebase
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    try {
                        if (snapshot.getValue() == null) {
                            nullCount++;
                            Log.d(TAG, "Bỏ qua phần tử null ở key: " + snapshot.getKey());
                            continue;
                        }
                        
                        Map<String, Object> map = (Map<String, Object>) snapshot.getValue();
                        if (map != null) {
                            try {
                                processedCount++;
                                Log.d(TAG, "--- Xử lý phần tử #" + processedCount + " (key=" + snapshot.getKey() + ") ---");
                                
                                DatBan datBan = SyncUtils.convertMapToDatBan(map);
                                
                                if (datBan != null) {
                                    Log.d(TAG, "  maDatBan=" + datBan.getMaDatBan());
                                    Log.d(TAG, "  maKhachHang=[" + datBan.getMaKhachHang() + "]");
                                    Log.d(TAG, "  trangThai=" + datBan.getTrangThai() + " (cần: " + DatBan.TRANG_THAI_DA_DUYET + ")");
                                    Log.d(TAG, "  maKhachHang.equals()=" + maKhachHang.equals(datBan.getMaKhachHang()));
                                    Log.d(TAG, "  trangThai match=" + (datBan.getTrangThai() == DatBan.TRANG_THAI_DA_DUYET));
                                    
                                    // Lọc theo maKhachHang và trangThai = TRANG_THAI_DA_DUYET (1)
                                    if (maKhachHang.equals(datBan.getMaKhachHang()) && 
                                        datBan.getTrangThai() == DatBan.TRANG_THAI_DA_DUYET) {
                                        resultList.add(datBan);
                                        matchCount++;
                                        Log.d(TAG, "  ✓✓✓ MATCH! Thêm vào danh sách");
                                    } else {
                                        Log.d(TAG, "  ✗ KHÔNG MATCH - Bỏ qua");
                                    }
                                } else {
                                    Log.e(TAG, "  datBan = null sau khi convert");
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Lỗi parse đặt bàn: " + snapshot.getKey(), e);
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Lỗi xử lý snapshot: " + snapshot.getKey(), e);
                    }
                }
                
                Log.d(TAG, "========================================");
                Log.d(TAG, "=== KẾT QUẢ CUỐI CÙNG ===");
                Log.d(TAG, "- Tổng children: " + dataSnapshot.getChildrenCount());
                Log.d(TAG, "- Phần tử null: " + nullCount);
                Log.d(TAG, "- Đã xử lý: " + processedCount);
                Log.d(TAG, "- Matching: " + matchCount);
                Log.d(TAG, "========================================");
                
                // Callback về main thread
                final ArrayList<DatBan> finalList = resultList;
                runOnUiThread(() -> {
                    if (datBanListListener != null) {
                        Log.d(TAG, "🔔 Gọi callback với " + finalList.size() + " items");
                        datBanListListener.onListReceived(finalList);
                    }
                });
                
                // ✅ REAL-TIME MODE: KHÔNG remove listener để tiếp tục lắng nghe changes
                // Listener sẽ được remove khi gọi stopFirebaseDirectListener() hoặc activity destroyed
                Log.d(TAG, "📡 Listener vẫn active - sẽ tự động update khi data thay đổi");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "❌ Firebase listener cancelled", error.toException());
                runOnUiThread(() -> {
                    if (datBanListListener != null) {
                        datBanListListener.onError(error.toException());
                    }
                });
                
                // ✅ REAL-TIME MODE: Chỉ remove listener khi có lỗi critical
                // Để cho stopFirebaseDirectListener() handle cleanup
                Log.w(TAG, "⚠️ Listener cancelled - cần reconnect hoặc check permissions");
            }
        };
        
        // ✅ REAL-TIME MODE: Dùng addValueEventListener thay vì addListenerForSingleValueEvent
        // Listener sẽ tự động trigger mỗi khi data thay đổi trên Firebase
        if (forceFromServer) {
            Log.d(TAG, "🔄 Force reload từ server - invalidating cache...");
            // Workaround để force Firebase load từ server:
            // Go offline rồi online lại để clear cache connection
            runInBackground(() -> {
                try {
                    database.goOffline();
                    // Sleep ngắn để đảm bảo offline state được set
                    Thread.sleep(100);
                    database.goOnline();
                    Log.d(TAG, "✓ Firebase reconnected - cache invalidated");
                    
                    // Attach REAL-TIME listener sau khi reconnect
                    runOnUiThread(() -> {
                        Log.d(TAG, "📡 Attaching REAL-TIME listener (addValueEventListener)");
                        dbRef.child(TABLE_NAME).addValueEventListener(firebaseDirectListenerDatBan);
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Error during force reload", e);
                    // Fallback: attach listener anyway
                    runOnUiThread(() -> {
                        dbRef.child(TABLE_NAME).addValueEventListener(firebaseDirectListenerDatBan);
                    });
                }
            });
        } else {
            // ✅ REAL-TIME MODE: addValueEventListener sẽ tự động update khi data thay đổi
            Log.d(TAG, "📡 Attaching REAL-TIME listener (addValueEventListener)");
            dbRef.child(TABLE_NAME).addValueEventListener(firebaseDirectListenerDatBan);
        }
    }

    /**
     * Dừng lắng nghe từ Firebase
     */
    public void stopFirebaseDirectListener() {
        if (firebaseDirectListenerDatBan != null) {
            dbRef.child(TABLE_NAME).removeEventListener(firebaseDirectListenerDatBan);
            firebaseDirectListenerDatBan = null;
        }
        datBanListListener = null;
    }
}

