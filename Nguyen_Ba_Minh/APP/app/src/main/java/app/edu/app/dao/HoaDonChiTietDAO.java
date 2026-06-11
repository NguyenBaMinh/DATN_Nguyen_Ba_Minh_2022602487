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
import app.edu.app.model.HoaDon;
import app.edu.app.model.HoaDonChiTiet;
import app.edu.app.model.HoaDonMangVe;
import app.edu.app.utils.SyncUtils;
import app.edu.app.utils.XDate;

public class HoaDonChiTietDAO extends FirebaseDAO {
    private static final String TABLE_NAME = "HoaDonChiTiet";
    private static final String TAG = "HoaDonChiTietDAO";
    CoffeeDB coffeeDB;

    public HoaDonChiTietDAO(Context context) {
        super(context);
        this.coffeeDB = new CoffeeDB(context);
        
        // Enable keepSynced để Firebase tự động sync node HoaDonChiTiet
        try {
            dbRef.child(TABLE_NAME).keepSynced(true);
            Log.d(TAG, "✓ Enabled keepSynced for HoaDonChiTiet node");
        } catch (Exception e) {
            Log.e(TAG, "Failed to enable keepSynced", e);
        }
    }

    @SuppressLint("Range")
    public ArrayList<HoaDonChiTiet> get(String sql, String... choose) {
        ArrayList<HoaDonChiTiet> list = new ArrayList<>();
        SQLiteDatabase sqLiteDatabase = coffeeDB.getReadableDatabase();
        @SuppressLint("Recycle") Cursor cursor = sqLiteDatabase.rawQuery(sql, choose);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            do {
                HoaDonChiTiet hoaDonChiTiet = new HoaDonChiTiet();
                hoaDonChiTiet.setMaHDCT(cursor.getInt(cursor.getColumnIndex("maHDCT")));
                hoaDonChiTiet.setMaHoaDon(cursor.getInt(cursor.getColumnIndex("maHoaDon")));
                hoaDonChiTiet.setMaHangHoa(cursor.getInt(cursor.getColumnIndex("maHangHoa")));
                hoaDonChiTiet.setSoLuong(cursor.getInt(cursor.getColumnIndex("soLuong")));
                hoaDonChiTiet.setGiaTien(cursor.getInt(cursor.getColumnIndex("giaTien")));
                hoaDonChiTiet.setGhiChu(cursor.getString(cursor.getColumnIndex("ghiChu")));
                try {
                    hoaDonChiTiet.setNgayXuatHoaDon(XDate.toDate(cursor.getString(cursor.getColumnIndex("ngayXuatHoaDon"))));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                list.add(hoaDonChiTiet);
                Log.i("TAG", hoaDonChiTiet.toString());
            } while (cursor.moveToNext());
        }

        return list;
    }

    public ArrayList<HoaDonChiTiet> getAll() {
        String sql = "SELECT * FROM HOADONCHITIET";
        ArrayList<HoaDonChiTiet> list = get(sql);

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
                            HoaDonChiTiet firebaseHDCT = SyncUtils.convertMapToHoaDonChiTiet(map);

                            // Check if item exists locally
                            try {
                                HoaDonChiTiet localHDCT = getByMaHDCT(String.valueOf(firebaseHDCT.getMaHDCT()));

                                // If local version is different, update it
                                if (localHDCT.getMaHoaDon() != firebaseHDCT.getMaHoaDon() ||
                                        localHDCT.getMaHangHoa() != firebaseHDCT.getMaHangHoa() ||
                                        localHDCT.getSoLuong() != firebaseHDCT.getSoLuong() ||
                                        localHDCT.getGiaTien() != firebaseHDCT.getGiaTien()) {

                                    updateHoaDonChiTiet(firebaseHDCT);
                                }
                            } catch (Exception e) {
                                // Item doesn't exist locally, insert it
                                insertHoaDonChiTiet(firebaseHDCT);
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

    public boolean insertHoaDonChiTiet(HoaDonChiTiet hoaDonChiTiet) {
        SQLiteDatabase sqLiteDatabase = coffeeDB.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("maHoaDon", hoaDonChiTiet.getMaHoaDon());
        values.put("maHangHoa", hoaDonChiTiet.getMaHangHoa());
        values.put("soLuong", hoaDonChiTiet.getSoLuong());
        values.put("giaTien", hoaDonChiTiet.getGiaTien());
        values.put("ghiChu", hoaDonChiTiet.getGhiChu());
        values.put("ngayXuatHoaDon", XDate.toStringDate(hoaDonChiTiet.getNgayXuatHoaDon()));
        long check = sqLiteDatabase.insert("HOADONCHITIET", null, values);

        // If local insert successful and network available, sync to Firebase
        if (check != -1 && isNetworkAvailable()) {
            // Get the auto-generated ID and set it to the object
            if (hoaDonChiTiet.getMaHDCT() == 0) {
                String query = "SELECT last_insert_rowid() as lastId";
                Cursor cursor = sqLiteDatabase.rawQuery(query, null);
                if (cursor.moveToFirst()) {
                    @SuppressLint("Range") int lastId = cursor.getInt(cursor.getColumnIndex("lastId"));
                    hoaDonChiTiet.setMaHDCT(lastId);
                }
                cursor.close();
            }

            syncToFirebase(hoaDonChiTiet);
        }

        return check != -1;
    }

    public boolean updateHoaDonChiTiet(HoaDonChiTiet hoaDonChiTiet) {
        SQLiteDatabase sqLiteDatabase = coffeeDB.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("maHoaDon", hoaDonChiTiet.getMaHoaDon());
        values.put("maHangHoa", hoaDonChiTiet.getMaHangHoa());
        values.put("soLuong", hoaDonChiTiet.getSoLuong());
        values.put("giaTien", hoaDonChiTiet.getGiaTien());
        values.put("ghiChu", hoaDonChiTiet.getGhiChu());
        values.put("ngayXuatHoaDon", XDate.toStringDate(hoaDonChiTiet.getNgayXuatHoaDon()));
        long check = sqLiteDatabase.update("HOADONCHITIET", values, "maHDCT=?", new String[]{String.valueOf(hoaDonChiTiet.getMaHDCT())});

        // If local update successful and network available, sync to Firebase
        if (check > 0 && isNetworkAvailable()) {
            syncToFirebase(hoaDonChiTiet);
        }

        return check > 0;
    }

    public boolean deleteHoaDonChiTiet(String maHDCT) {
        SQLiteDatabase sqLiteDatabase = coffeeDB.getWritableDatabase();
        int check = sqLiteDatabase.delete("HOADONCHITIET", "maHDCT=?", new String[]{maHDCT});

        // If local delete successful and network available, delete from Firebase
        if (check > 0 && isNetworkAvailable()) {
            deleteFromFirebase(maHDCT);
        }

        return check > 0;
    }

    public boolean deleteHoaDonChiTietByMaHoaDon(String maHoaDon) {
        SQLiteDatabase sqLiteDatabase = coffeeDB.getWritableDatabase();
        int check = sqLiteDatabase.delete("HOADONCHITIET", "maHoaDon=?", new String[]{maHoaDon});

        // If local delete successful and network available, delete related records from Firebase
        if (check > 0 && isNetworkAvailable()) {
            // Find and delete all records with this maHoaDon from Firebase
            dbRef.child(TABLE_NAME).orderByChild("maHoaDon").equalTo(Integer.parseInt(maHoaDon))
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            for (DataSnapshot childSnapshot : dataSnapshot.getChildren()) {
                                childSnapshot.getRef().removeValue();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            Log.e(TAG, "Error deleting records by maHoaDon", databaseError.toException());
                        }
                    });
        }

        return check > 0;
    }

    public HoaDonChiTiet getByMaHDCT(String maHDCT) {
        String sql = "SELECT * FROM HOADONCHITIET WHERE maHDCT=?";
        ArrayList<HoaDonChiTiet> list = get(sql, maHDCT);

        return list.get(0);
    }

    public ArrayList<HoaDonChiTiet> getByMaHoaDon(String maHoaDon) {
        String sql = "SELECT * FROM HOADONCHITIET WHERE maHoaDon=?";
        return get(sql, maHoaDon);
    }

    @SuppressLint("Range")
    public int getDoanhThuNgay(String date) {
        SQLiteDatabase sqLiteDatabase = coffeeDB.getReadableDatabase();
        ArrayList<Integer> list = new ArrayList<>();
        int totalRevenue = 0;

        // Get revenue for regular approved invoices
        String sqlRegular = "SELECT SUM(hct.giaTien) as DoanhThu FROM HOADONCHITIET hct " +
                "INNER JOIN HOADON hd ON hct.maHoaDon = hd.maHoaDon " +
                "WHERE hct.ngayXuatHoaDon=? AND hd.trangThai=?";

        Cursor cursorRegular = sqLiteDatabase.rawQuery(sqlRegular, new String[]{date, String.valueOf(HoaDon.DA_THANH_TOAN)});
        try {
            if (cursorRegular.moveToFirst()) {
                totalRevenue += cursorRegular.getInt(cursorRegular.getColumnIndex("DoanhThu"));
            }
        } finally {
            if (cursorRegular != null && !cursorRegular.isClosed()) {
                cursorRegular.close();
            }
        }

        // Get revenue for takeaway approved invoices
        String sqlTakeaway = "SELECT SUM(hct.giaTien) as DoanhThu FROM HOADONCHITIET hct " +
                "INNER JOIN HOADONMANGVE hdmv ON hct.maHoaDon = hdmv.maHoaDon " +
                "WHERE hct.ngayXuatHoaDon=? AND hdmv.trangThai=?";

        Cursor cursorTakeaway = sqLiteDatabase.rawQuery(sqlTakeaway, new String[]{date, String.valueOf(HoaDonMangVe.DA_DUYET)});
        try {
            if (cursorTakeaway.moveToFirst()) {
                totalRevenue += cursorTakeaway.getInt(cursorTakeaway.getColumnIndex("DoanhThu"));
            }
        } finally {
            if (cursorTakeaway != null && !cursorTakeaway.isClosed()) {
                cursorTakeaway.close();
            }
        }

        return totalRevenue;
    }

    @SuppressLint("Range")
    public int getDTThangNam(String tuNgay, String denNgay) {
        SQLiteDatabase sqLiteDatabase = coffeeDB.getReadableDatabase();
        int totalRevenue = 0;

        // Get revenue for regular approved invoices
        String sqlRegular = "SELECT SUM(hct.giaTien) as doanhThu FROM HOADONCHITIET hct " +
                "INNER JOIN HOADON hd ON hct.maHoaDon = hd.maHoaDon " +
                "WHERE hct.ngayXuatHoaDon BETWEEN ? AND ? AND hd.trangThai=?";

        Cursor cursorRegular = sqLiteDatabase.rawQuery(sqlRegular,
                new String[]{tuNgay, denNgay, String.valueOf(HoaDon.DA_THANH_TOAN)});
        try {
            if (cursorRegular.moveToFirst()) {
                totalRevenue += cursorRegular.getInt(cursorRegular.getColumnIndex("doanhThu"));
            }
        } finally {
            if (cursorRegular != null && !cursorRegular.isClosed()) {
                cursorRegular.close();
            }
        }

        // Get revenue for takeaway approved invoices
        String sqlTakeaway = "SELECT SUM(hct.giaTien) as doanhThu FROM HOADONCHITIET hct " +
                "INNER JOIN HOADONMANGVE hdmv ON hct.maHoaDon = hdmv.maHoaDon " +
                "WHERE hct.ngayXuatHoaDon BETWEEN ? AND ? AND hdmv.trangThai=?";

        Cursor cursorTakeaway = sqLiteDatabase.rawQuery(sqlTakeaway,
                new String[]{tuNgay, denNgay, String.valueOf(HoaDonMangVe.DA_DUYET)});
        try {
            if (cursorTakeaway.moveToFirst()) {
                totalRevenue += cursorTakeaway.getInt(cursorTakeaway.getColumnIndex("doanhThu"));
            }
        } finally {
            if (cursorTakeaway != null && !cursorTakeaway.isClosed()) {
                cursorTakeaway.close();
            }
        }

        return totalRevenue;
    }

    @SuppressLint("Range")
    public int getGiaTien(int maHoaDon) {
        SQLiteDatabase sqLiteDatabase = coffeeDB.getReadableDatabase();

        // Just directly calculate the revenue for the invoice ID
        String sql = "SELECT SUM(giaTien) as DoanhThu FROM HOADONCHITIET WHERE maHoaDon=?";
        ArrayList<Integer> list = new ArrayList<>();
        Cursor cursor = sqLiteDatabase.rawQuery(sql, new String[]{String.valueOf(maHoaDon)});
        try {
            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                do {
                    try {
                        list.add(cursor.getInt(cursor.getColumnIndex("DoanhThu")));
                    } catch (Exception e) {
                        list.add(0);
                    }
                } while (cursor.moveToNext());
            } else {
                // Add 0 when no results found
                list.add(0);
            }
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
        // Check if list is empty before accessing index 0
        return list.isEmpty() ? 0 : list.get(0);
    }

    // Add this at class level
    private RealtimeUpdateCallback hoaDonChiTietCallback;

    /**
     * Start real-time synchronization for HoaDonChiTiet table
     */
    public void startRealtimeSync() {
        hoaDonChiTietCallback = new RealtimeUpdateCallback() {
            @Override
            public void onDataChanged(DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    try {
                        Map<String, Object> map = (Map<String, Object>) snapshot.getValue();
                        if (map != null) {
                            HoaDonChiTiet firebaseHDCT = SyncUtils.convertMapToHoaDonChiTiet(map);

                            try {
                                HoaDonChiTiet localHDCT = getByMaHDCT(String.valueOf(firebaseHDCT.getMaHDCT()));
                                // Update if needed
                                updateHoaDonChiTietFromFirebase(firebaseHDCT);
                            } catch (Exception e) {
                                // Item doesn't exist locally, insert it
                                insertHoaDonChiTietFromFirebase(firebaseHDCT);
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing Firebase HoaDonChiTiet update", e);
                    }
                }
            }
        };

        setupRealtimeListener(TABLE_NAME, hoaDonChiTietCallback);
    }

    /**
     * Stop real-time synchronization
     */
    public void stopRealtimeSync() {
        removeRealtimeListener(TABLE_NAME);
    }

    // Firebase update methods
    private boolean updateHoaDonChiTietFromFirebase(HoaDonChiTiet hoaDonChiTiet) {
        SQLiteDatabase sqLiteDatabase = coffeeDB.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("maHoaDon", hoaDonChiTiet.getMaHoaDon());
        values.put("maHangHoa", hoaDonChiTiet.getMaHangHoa());
        values.put("soLuong", hoaDonChiTiet.getSoLuong());
        values.put("giaTien", hoaDonChiTiet.getGiaTien());
        values.put("ghiChu", hoaDonChiTiet.getGhiChu());
        values.put("ngayXuatHoaDon", XDate.toStringDate(hoaDonChiTiet.getNgayXuatHoaDon()));

        int check = sqLiteDatabase.update("HOADONCHITIET", values, "maHDCT=?",
                new String[]{String.valueOf(hoaDonChiTiet.getMaHDCT())});

        if (check > 0) {
            Log.d(TAG, "Updated HoaDonChiTiet from Firebase: " + hoaDonChiTiet.getMaHDCT());
        }

        return check > 0;
    }

    private boolean insertHoaDonChiTietFromFirebase(HoaDonChiTiet hoaDonChiTiet) {
        SQLiteDatabase sqLiteDatabase = coffeeDB.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("maHDCT", hoaDonChiTiet.getMaHDCT());
        values.put("maHoaDon", hoaDonChiTiet.getMaHoaDon());
        values.put("maHangHoa", hoaDonChiTiet.getMaHangHoa());
        values.put("soLuong", hoaDonChiTiet.getSoLuong());
        values.put("giaTien", hoaDonChiTiet.getGiaTien());
        values.put("ghiChu", hoaDonChiTiet.getGhiChu());
        values.put("ngayXuatHoaDon", XDate.toStringDate(hoaDonChiTiet.getNgayXuatHoaDon()));

        long check = sqLiteDatabase.insert("HOADONCHITIET", null, values);

        if (check != -1) {
            Log.d(TAG, "Inserted HoaDonChiTiet from Firebase: " + hoaDonChiTiet.getMaHDCT());
        }

        return check != -1;
    }

    //lấy 5 sản phẩm bán chạy nhất trong ngày
    @SuppressLint("Range")
    public ArrayList<HoaDonChiTiet> getTop5BestSellingProducts(String date) {
        SQLiteDatabase sqLiteDatabase = coffeeDB.getReadableDatabase();
        String sql = "SELECT maHangHoa, SUM(soLuong) as totalSold FROM HOADONCHITIET WHERE ngayXuatHoaDon=? GROUP BY maHangHoa ORDER BY totalSold DESC LIMIT 5";
        ArrayList<HoaDonChiTiet> list = new ArrayList<>();

        Cursor cursor = sqLiteDatabase.rawQuery(sql, new String[]{date});
        try {
            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                do {
                    HoaDonChiTiet hoaDonChiTiet = new HoaDonChiTiet();
                    hoaDonChiTiet.setMaHangHoa(cursor.getInt(cursor.getColumnIndex("maHangHoa")));
                    hoaDonChiTiet.setSoLuong(cursor.getInt(cursor.getColumnIndex("totalSold")));
                    list.add(hoaDonChiTiet);
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
        return list;
    }

    @SuppressLint("Range")
    public ArrayList<HoaDonChiTiet> getTopSellingProductsInMonth(String tuNgay, String denNgay) {
        SQLiteDatabase sqLiteDatabase = coffeeDB.getReadableDatabase();
        String sql = "SELECT maHangHoa, SUM(soLuong) as totalSold FROM HOADONCHITIET WHERE ngayXuatHoaDon BETWEEN ? AND ? GROUP BY maHangHoa ORDER BY totalSold DESC LIMIT 5";
        ArrayList<HoaDonChiTiet> list = new ArrayList<>();

        Cursor cursor = sqLiteDatabase.rawQuery(sql, new String[]{tuNgay, denNgay});
        try {
            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                do {
                    HoaDonChiTiet hoaDonChiTiet = new HoaDonChiTiet();
                    hoaDonChiTiet.setMaHangHoa(cursor.getInt(cursor.getColumnIndex("maHangHoa")));
                    hoaDonChiTiet.setSoLuong(cursor.getInt(cursor.getColumnIndex("totalSold")));
                    list.add(hoaDonChiTiet);
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
        return list;
    }

    @SuppressLint("Range")
    public ArrayList<HoaDonChiTiet> getTopSellingProductsInYear(String date) {
        SQLiteDatabase sqLiteDatabase = coffeeDB.getReadableDatabase();
        String sql = "SELECT maHangHoa, SUM(soLuong) as totalSold FROM HOADONCHITIET WHERE ngayXuatHoaDon LIKE ? GROUP BY maHangHoa ORDER BY totalSold DESC LIMIT 5";
        ArrayList<HoaDonChiTiet> list = new ArrayList<>();

        Cursor cursor = sqLiteDatabase.rawQuery(sql, new String[]{date});
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            do {
                HoaDonChiTiet hoaDonChiTiet = new HoaDonChiTiet();
                hoaDonChiTiet.setMaHangHoa(cursor.getInt(cursor.getColumnIndex("maHangHoa")));
                hoaDonChiTiet.setSoLuong(cursor.getInt(cursor.getColumnIndex("totalSold")));
                list.add(hoaDonChiTiet);
            } while (cursor.moveToNext());
        }
        return list;
    }

    // Sync a HoaDonChiTiet object to Firebase
    private void syncToFirebase(HoaDonChiTiet hoaDonChiTiet) {
        Map<String, Object> hoaDonChiTietMap = SyncUtils.convertHoaDonChiTietToMap(hoaDonChiTiet);

        dbRef.child(TABLE_NAME).child(String.valueOf(hoaDonChiTiet.getMaHDCT()))
                .setValue(hoaDonChiTietMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        logSuccessfulSync("Sync", String.valueOf(hoaDonChiTiet.getMaHDCT()));
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        handleSyncError("Sync", String.valueOf(hoaDonChiTiet.getMaHDCT()), e);
                    }
                });
    }

    // Delete a HoaDonChiTiet from Firebase
    private void deleteFromFirebase(String maHDCT) {
        dbRef.child(TABLE_NAME).child(maHDCT)
                .removeValue()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        logSuccessfulSync("Delete", maHDCT);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        handleSyncError("Delete", maHDCT, e);
                    }
                });
    }

    /**
     * Interface để nhận callback khi lấy chi tiết hóa đơn từ Firebase
     */
    public interface OnHoaDonChiTietListListener {
        void onListReceived(ArrayList<HoaDonChiTiet> list);
        void onError(Exception e);
    }

    private ValueEventListener firebaseDirectListenerHDCT;
    private OnHoaDonChiTietListListener hoaDonChiTietListListener;
    private boolean isProcessingCallback = false; // ✅ Flag để tránh callback duplicate

    /**
     * Lấy chi tiết hóa đơn trực tiếp từ Firebase (không sync về SQLite)
     * ✅ REAL-TIME MODE: Sử dụng addValueEventListener để tự động update khi data thay đổi
     * ✅ Sử dụng Firebase Query để filter hiệu quả
     * 
     * @param maHoaDon Mã hóa đơn
     * @param listener Callback để nhận kết quả (sẽ được gọi lại mỗi khi data thay đổi)
     */
    public void getByMaHoaDonFromFirebaseDirect(int maHoaDon, OnHoaDonChiTietListListener listener) {
        Log.d(TAG, "╔══════════════════════════════════════╗");
        Log.d(TAG, "║  getByMaHoaDonFromFirebaseDirect     ║");
        Log.d(TAG, "╚══════════════════════════════════════╝");
        Log.d(TAG, "➤ Tìm chi tiết hóa đơn: maHoaDon=" + maHoaDon);
        Log.d(TAG, "firebaseDirectListenerHDCT hiện tại: " + (firebaseDirectListenerHDCT != null ? "CÓ" : "NULL"));
        
        this.hoaDonChiTietListListener = listener;
        
        // ✅ CRITICAL: Remove listener cũ để tránh callback duplicate
        if (firebaseDirectListenerHDCT != null) {
            Log.w(TAG, "⚠ Đang remove listener cũ để tránh duplicate!");
            dbRef.child(TABLE_NAME).removeEventListener(firebaseDirectListenerHDCT);
            firebaseDirectListenerHDCT = null;
        }
        
        // Tạo listener MỚI
        firebaseDirectListenerHDCT = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.d(TAG, "╔═══════════════════════════════════════════╗");
                Log.d(TAG, "║  🔔 REAL-TIME CALLBACK TRIGGERED          ║");
                Log.d(TAG, "╚═══════════════════════════════════════════╝");
                Log.d(TAG, "Firebase trả về: " + dataSnapshot.getChildrenCount() + " items");
                
                // ✅ REAL-TIME MODE: KHÔNG block callbacks
                // Mỗi thay đổi trên Firebase sẽ trigger callback để update UI
                // Firebase đã optimize để chỉ push changes, không phải toàn bộ data
                
                ArrayList<HoaDonChiTiet> resultList = new ArrayList<>();
                
                int processedCount = 0;
                int successCount = 0;
                int errorCount = 0;
                
                // ✅ Xử lý dữ liệu từ Firebase (đã được filter sẵn bởi Query)
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    try {
                        processedCount++;
                        
                        if (snapshot.getValue() == null) {
                            Log.w(TAG, "  [" + processedCount + "] Snapshot null, bỏ qua");
                            errorCount++;
                            continue;
                        }
                        
                        Map<String, Object> map = (Map<String, Object>) snapshot.getValue();
                        if (map != null) {
                            try {
                                HoaDonChiTiet hdct = parseHoaDonChiTietFromMap(map);
                                
                                // ✅ Không cần check maHoaDon nữa vì Firebase đã filter rồi
                                if (hdct != null) {
                                    resultList.add(hdct);
                                    successCount++;
                                    Log.d(TAG, "  [" + processedCount + "] ✓ maHDCT=" + hdct.getMaHDCT() + 
                                          ", maHoaDon=" + hdct.getMaHoaDon() + 
                                          ", maHangHoa=" + hdct.getMaHangHoa() + 
                                          ", soLuong=" + hdct.getSoLuong());
                                } else {
                                    Log.w(TAG, "  [" + processedCount + "] Parse trả về null");
                                    errorCount++;
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "  [" + processedCount + "] Lỗi parse: " + snapshot.getKey(), e);
                                errorCount++;
                            }
                        } else {
                            Log.w(TAG, "  [" + processedCount + "] Map null");
                            errorCount++;
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "  [" + processedCount + "] Lỗi xử lý snapshot: " + snapshot.getKey(), e);
                        errorCount++;
                    }
                }
                
                Log.d(TAG, "╔══════════════════════════════════════╗");
                Log.d(TAG, "║           KẾT QUẢ LOAD               ║");
                Log.d(TAG, "╚══════════════════════════════════════╝");
                Log.d(TAG, "Đã xử lý: " + processedCount + " items");
                Log.d(TAG, "Thành công: " + successCount + " items ✓");
                Log.d(TAG, "Lỗi: " + errorCount + " items ✗");
                Log.d(TAG, "Trả về: " + resultList.size() + " items");
                
                // In ra chi tiết từng item sẽ trả về
                Log.d(TAG, "Chi tiết danh sách trả về:");
                for (int i = 0; i < resultList.size(); i++) {
                    HoaDonChiTiet item = resultList.get(i);
                    Log.d(TAG, "  [" + i + "] maHDCT=" + item.getMaHDCT() + 
                          ", maHangHoa=" + item.getMaHangHoa() + 
                          ", soLuong=" + item.getSoLuong() +
                          ", giaTien=" + item.getGiaTien());
                }
                
                // ✅ CRITICAL: Remove listener TRƯỚC khi callback để tránh re-trigger
                if (firebaseDirectListenerHDCT != null) {
                    Log.d(TAG, "▶ Remove Firebase listener");
                    dbRef.child(TABLE_NAME)
                         .orderByChild("maHoaDon")
                         .equalTo(maHoaDon)
                         .removeEventListener(firebaseDirectListenerHDCT);
                    firebaseDirectListenerHDCT = null;
                }
                
                // Callback về main thread
                final ArrayList<HoaDonChiTiet> finalList = resultList;
                Log.d(TAG, "▶ Gọi callback với " + finalList.size() + " items");
                runOnUiThread(() -> {
                    if (hoaDonChiTietListListener != null) {
                        Log.d(TAG, "🔔 Triggering UI update với " + finalList.size() + " món");
                        hoaDonChiTietListListener.onListReceived(finalList);
                        Log.d(TAG, "✓ Callback complete - UI sẽ được update");
                    } else {
                        Log.w(TAG, "⚠ Listener is NULL, không thể callback!");
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "❌ Firebase listener cancelled", error.toException());
                
                runOnUiThread(() -> {
                    if (hoaDonChiTietListListener != null) {
                        hoaDonChiTietListListener.onError(error.toException());
                    }
                });
                
                // ✅ REAL-TIME MODE: Không remove listener, để stopFirebaseDirectListener() handle cleanup
                // Listener sẽ tiếp tục active để lắng nghe changes
                Log.w(TAG, "⚠️ Listener cancelled - check permissions hoặc network");
            }
        };
        
        // ✅ REAL-TIME MODE: Sử dụng addValueEventListener thay vì addListenerForSingleValueEvent
        // Listener sẽ tự động trigger mỗi khi data thay đổi trên Firebase
        // Query với orderByChild().equalTo() để filter server-side
        Log.d(TAG, "▶ Attach REAL-TIME listener: orderByChild('maHoaDon').equalTo(" + maHoaDon + ")");
        Log.d(TAG, "📡 Mode: addValueEventListener - sẽ tự động update khi data thay đổi");
        dbRef.child(TABLE_NAME)
             .orderByChild("maHoaDon")  // ✅ Index theo maHoaDon
             .equalTo(maHoaDon)          // ✅ Chỉ lấy matching records
             .addValueEventListener(firebaseDirectListenerHDCT);
    }

    /**
     * Parse HoaDonChiTiet từ Map
     */
    private HoaDonChiTiet parseHoaDonChiTietFromMap(Map<String, Object> map) {
        try {
            HoaDonChiTiet hdct = new HoaDonChiTiet();
            
            // Parse maHDCT
            Object maHDCTObj = map.get("maHDCT");
            if (maHDCTObj instanceof Long) {
                hdct.setMaHDCT(((Long) maHDCTObj).intValue());
            } else if (maHDCTObj instanceof Integer) {
                hdct.setMaHDCT((Integer) maHDCTObj);
            } else if (maHDCTObj instanceof Double) {
                hdct.setMaHDCT(((Double) maHDCTObj).intValue());
            }
            
            // Parse maHoaDon
            Object maHoaDonObj = map.get("maHoaDon");
            if (maHoaDonObj instanceof Long) {
                hdct.setMaHoaDon(((Long) maHoaDonObj).intValue());
            } else if (maHoaDonObj instanceof Integer) {
                hdct.setMaHoaDon((Integer) maHoaDonObj);
            } else if (maHoaDonObj instanceof Double) {
                hdct.setMaHoaDon(((Double) maHoaDonObj).intValue());
            }
            
            // Parse maHangHoa
            Object maHangHoaObj = map.get("maHangHoa");
            if (maHangHoaObj instanceof Long) {
                hdct.setMaHangHoa(((Long) maHangHoaObj).intValue());
            } else if (maHangHoaObj instanceof Integer) {
                hdct.setMaHangHoa((Integer) maHangHoaObj);
            } else if (maHangHoaObj instanceof Double) {
                hdct.setMaHangHoa(((Double) maHangHoaObj).intValue());
            }
            
            // Parse soLuong
            Object soLuongObj = map.get("soLuong");
            if (soLuongObj instanceof Long) {
                hdct.setSoLuong(((Long) soLuongObj).intValue());
            } else if (soLuongObj instanceof Integer) {
                hdct.setSoLuong((Integer) soLuongObj);
            } else if (soLuongObj instanceof Double) {
                hdct.setSoLuong(((Double) soLuongObj).intValue());
            }
            
            // Parse giaTien
            Object giaTienObj = map.get("giaTien");
            if (giaTienObj instanceof Long) {
                hdct.setGiaTien(((Long) giaTienObj).intValue());
            } else if (giaTienObj instanceof Integer) {
                hdct.setGiaTien((Integer) giaTienObj);
            } else if (giaTienObj instanceof Double) {
                hdct.setGiaTien(((Double) giaTienObj).intValue());
            }
            
            // Parse ghiChu
            hdct.setGhiChu((String) map.get("ghiChu"));
            
            // Parse ngayXuatHoaDon
            try {
                String ngayXuatHoaDonStr = (String) map.get("ngayXuatHoaDon");
                if (ngayXuatHoaDonStr != null) {
                    hdct.setNgayXuatHoaDon(XDate.toDate(ngayXuatHoaDonStr));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error parsing ngayXuatHoaDon", e);
            }
            
            return hdct;
        } catch (Exception e) {
            Log.e(TAG, "CRITICAL: Error parsing HoaDonChiTiet from map", e);
            return null;
        }
    }

    /**
     * Dừng lắng nghe từ Firebase
     */
    public void stopFirebaseDirectListener() {
        if (firebaseDirectListenerHDCT != null) {
            dbRef.child(TABLE_NAME).removeEventListener(firebaseDirectListenerHDCT);
            firebaseDirectListenerHDCT = null;
        }
        hoaDonChiTietListListener = null;
    }
}