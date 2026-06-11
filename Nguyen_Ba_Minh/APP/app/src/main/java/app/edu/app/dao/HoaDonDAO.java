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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Map;

import app.edu.app.database.CoffeeDB;
import app.edu.app.interfaces.RealtimeUpdateCallback;
import app.edu.app.utils.XDate;
import app.edu.app.model.HoaDon;
import app.edu.app.utils.SyncUtils;

public class HoaDonDAO extends FirebaseDAO {
    private static final String TABLE_NAME = "HoaDon";
    CoffeeDB coffeeDB;
    @SuppressLint("SimpleDateFormat")
    SimpleDateFormat spf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
    String TAG = "HoaDonDAO";
    public HoaDonDAO(Context context) {
        super(context);
        this.coffeeDB = new CoffeeDB(context);
        
        // Enable keepSynced để Firebase tự động sync node HoaDon
        try {
            dbRef.child(TABLE_NAME).keepSynced(true);
            Log.d(TAG, "✓ Enabled keepSynced for HoaDon node");
        } catch (Exception e) {
            Log.e(TAG, "Failed to enable keepSynced", e);
        }
    }

    // Add this at class level
    private RealtimeUpdateCallback hoaDonCallback;

    /**
     * Start real-time synchronization for HoaDon table
     */
    public void startRealtimeSync() {
        hoaDonCallback = new RealtimeUpdateCallback() {
            @Override
            public void onDataChanged(DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    try {
                        Map<String, Object> map = (Map<String, Object>) snapshot.getValue();
                        if (map != null) {
                            HoaDon firebaseHoaDon = SyncUtils.convertMapToHoaDon(map);

                            try {
                                HoaDon localHoaDon = getByMaHoaDon(String.valueOf(firebaseHoaDon.getMaHoaDon()));
                                // Update if needed
                                updateHoaDonFromFirebase(firebaseHoaDon);
                            } catch (Exception e) {
                                // Item doesn't exist locally, insert it
                                insertHoaDonFromFirebase(firebaseHoaDon);
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing Firebase HoaDon update", e);
                    }
                }
            }
        };

        setupRealtimeListener(TABLE_NAME, hoaDonCallback);
    }

    /**
     * Stop real-time synchronization
     */
    public void stopRealtimeSync() {
        removeRealtimeListener(TABLE_NAME);
    }

    // Firebase update methods
    private boolean updateHoaDonFromFirebase(HoaDon hoaDon) {
        SQLiteDatabase sqLiteDatabase = coffeeDB.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("maBan", hoaDon.getMaBan());
        values.put("gioVao", XDate.toStringDateTime(hoaDon.getGioVao()));
        values.put("gioRa", XDate.toStringDateTime(hoaDon.getGioRa()));
        values.put("trangThai", hoaDon.getTrangThai());
        values.put("maKhachHang", hoaDon.getMaKhachHang());
        values.put("ghiChu", hoaDon.getGhiChu());

        int check = sqLiteDatabase.update("HOADON", values, "maHoaDon=?",
                new String[]{String.valueOf(hoaDon.getMaHoaDon())});

        if (check > 0) {
            Log.d(TAG, "Updated HoaDon from Firebase: " + hoaDon.getMaHoaDon());
        }

        return check > 0;
    }

    private boolean insertHoaDonFromFirebase(HoaDon hoaDon) {
        SQLiteDatabase sqLiteDatabase = coffeeDB.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("maHoaDon", hoaDon.getMaHoaDon());
        values.put("maBan", hoaDon.getMaBan());
        values.put("gioVao", XDate.toStringDateTime(hoaDon.getGioVao()));
        values.put("gioRa", XDate.toStringDateTime(hoaDon.getGioRa()));
        values.put("trangThai", hoaDon.getTrangThai());
        values.put("maKhachHang", hoaDon.getMaKhachHang());
        values.put("ghiChu", hoaDon.getGhiChu());

        long check = sqLiteDatabase.insert("HOADON", null, values);

        if (check != -1) {
            Log.d(TAG, "Inserted HoaDon from Firebase: " + hoaDon.getMaHoaDon());
        }

        return check != -1;
    }

    @SuppressLint("Range")
    public ArrayList<HoaDon> get(String sql, String... choose) {
        ArrayList<HoaDon> list = new ArrayList<>();
        SQLiteDatabase sqLiteDatabase = coffeeDB.getReadableDatabase();
        @SuppressLint("Recycle") Cursor cursor = sqLiteDatabase.rawQuery(sql, choose);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            do {
                HoaDon hoaDon = new HoaDon();
                hoaDon.setMaHoaDon(cursor.getInt(cursor.getColumnIndex("maHoaDon")));
                hoaDon.setMaBan(cursor.getInt(cursor.getColumnIndex("maBan")));
                try {
                    hoaDon.setGioVao(XDate.toDateTime(cursor.getString(cursor.getColumnIndex("gioVao"))));
                    hoaDon.setGioRa(XDate.toDateTime(cursor.getString(cursor.getColumnIndex("gioRa"))));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                hoaDon.setTrangThai(cursor.getInt(cursor.getColumnIndex("trangThai")));
                hoaDon.setMaKhachHang(cursor.getString(cursor.getColumnIndex("maKhachHang")));
                hoaDon.setGhiChu(cursor.getString(cursor.getColumnIndex("ghiChu")));
                list.add(hoaDon);
                Log.i("TAG", hoaDon.toString());
            } while (cursor.moveToNext());
        }

        return list;
    }

    public ArrayList<HoaDon> getAll() {
        String sql = "SELECT * FROM HOADON";
        ArrayList<HoaDon> list = get(sql);

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
                            HoaDon firebaseHoaDon = SyncUtils.convertMapToHoaDon(map);

                            // Check if item exists locally
                            try {
                                HoaDon localHoaDon = getByMaHoaDon(String.valueOf(firebaseHoaDon.getMaHoaDon()));

                                // If local version is different, update it
                                if (localHoaDon.getMaBan() != firebaseHoaDon.getMaBan() ||
                                        localHoaDon.getTrangThai() != firebaseHoaDon.getTrangThai() ||
                                        !localHoaDon.getMaKhachHang().equals(firebaseHoaDon.getMaKhachHang())) {

                                    updateHoaDon(firebaseHoaDon);
                                }
                            } catch (Exception e) {
                                // Item doesn't exist locally, insert it
                                insertHoaDon(firebaseHoaDon);
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

    public boolean insertHoaDon(HoaDon hoaDon) {
        SQLiteDatabase sqLiteDatabase = coffeeDB.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("maBan", hoaDon.getMaBan());
        values.put("gioVao", XDate.toStringDateTime(hoaDon.getGioVao()));
        values.put("gioRa", XDate.toStringDateTime(hoaDon.getGioRa()));
        values.put("trangThai", hoaDon.getTrangThai());
        values.put("maKhachHang", hoaDon.getMaKhachHang());
        values.put("ghiChu", hoaDon.getGhiChu());
        long check = sqLiteDatabase.insert("HOADON", null, values);

        // If local insert successful and network available, sync to Firebase
        if (check != -1 && isNetworkAvailable()) {
            // Get the auto-generated ID and set it to the object
            if (hoaDon.getMaHoaDon() == 0) {
                String query = "SELECT last_insert_rowid() as lastId";
                Cursor cursor = sqLiteDatabase.rawQuery(query, null);
                if (cursor.moveToFirst()) {
                    @SuppressLint("Range") int lastId = cursor.getInt(cursor.getColumnIndex("lastId"));
                    hoaDon.setMaHoaDon(lastId);
                }
                cursor.close();
            }

            syncToFirebase(hoaDon);
        }

        return check != -1;
    }

    public boolean updateHoaDon(HoaDon hoaDon) {
        SQLiteDatabase sqLiteDatabase = coffeeDB.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("maBan", hoaDon.getMaBan());
        values.put("gioVao", XDate.toStringDateTime(hoaDon.getGioVao()));
        values.put("gioRa", XDate.toStringDateTime(hoaDon.getGioRa()));
        values.put("trangThai", hoaDon.getTrangThai());
        values.put("maKhachHang", hoaDon.getMaKhachHang());
        values.put("ghiChu", hoaDon.getGhiChu());
        int check = sqLiteDatabase.update("HOADON", values, "maHoaDon=?", new String[]{String.valueOf(hoaDon.getMaHoaDon())});

        // If local update successful and network available, sync to Firebase
        if (check > 0 && isNetworkAvailable()) {
            syncToFirebase(hoaDon);
        }

        return check > 0;
    }

    public boolean deleteHoaDon(String maHoaDon) {
        SQLiteDatabase sqLiteDatabase = coffeeDB.getWritableDatabase();
        int check = sqLiteDatabase.delete("HOADON", "maHoaDon=?", new String[]{String.valueOf(maHoaDon)});

        // If local delete successful and network available, delete from Firebase
        if (check > 0 && isNetworkAvailable()) {
            deleteFromFirebase(maHoaDon);
        }

        return check > 0;
    }

    public HoaDon getByMaHoaDon(String maHoaDon) {
        String sql = "SELECT * FROM HOADON WHERE maHoaDon=?";
        ArrayList<HoaDon> list = get(sql, maHoaDon);

        return list.get(0);
    }

    /**
     * Lấy hóa đơn chưa thanh toán của một bàn
     * 
     * LƯU Ý:
     * - Mỗi bàn chỉ nên có một hóa đơn chưa thanh toán đang active
     * - Nếu có nhiều hóa đơn, lấy hóa đơn mới nhất (theo maHoaDon DESC)
     * 
     * @param maBan Mã bàn
     * @param trangThai Trạng thái hóa đơn (0 = chưa thanh toán)
     * @return Hóa đơn mới nhất chưa thanh toán của bàn, null nếu không có
     */
    public HoaDon getByMaHoaDonVaTrangThai(String maBan, int trangThai) {
        // Sắp xếp theo maHoaDon DESC để lấy hóa đơn mới nhất
        String sql = "SELECT * FROM HOADON WHERE maBan=? AND trangThai=? ORDER BY maHoaDon DESC";
        ArrayList<HoaDon> list = get(sql, maBan, String.valueOf(trangThai));

        if (list == null || list.isEmpty()) {
            return null;
        }
        
        // Trả về hóa đơn mới nhất (đầu tiên sau khi sắp xếp DESC)
        return list.get(0);
    }
    
    /**
     * Lấy hóa đơn chưa thanh toán của một bàn theo khách hàng
     * 
     * Dùng khi khách hàng mở từ QuanLyBanNguoiDungActivity để đảm bảo
     * chỉ lấy hóa đơn của chính khách hàng đó, không lấy nhầm của khách khác
     * 
     * @param maBan Mã bàn
     * @param maKhachHang Mã khách hàng
     * @param trangThai Trạng thái hóa đơn (0 = chưa thanh toán)
     * @return Hóa đơn mới nhất chưa thanh toán của bàn và khách hàng, null nếu không có
     */
    public HoaDon getByMaBanVaMaKhachHangVaTrangThai(String maBan, String maKhachHang, int trangThai) {
        // Sắp xếp theo maHoaDon DESC để lấy hóa đơn mới nhất
        String sql = "SELECT * FROM HOADON WHERE maBan=? AND maKhachHang=? AND trangThai=? ORDER BY maHoaDon DESC";
        ArrayList<HoaDon> list = get(sql, maBan, maKhachHang, String.valueOf(trangThai));

        if (list == null || list.isEmpty()) {
            return null;
        }
        
        // Trả về hóa đơn mới nhất (đầu tiên sau khi sắp xếp DESC)
        return list.get(0);
    }

    public ArrayList<HoaDon> getByTrangThai(int status) {
        String sql = "SELECT * FROM HOADON WHERE trangThai=?";

        return get(sql, String.valueOf(status));
    }

    //get all by maKhachHang
    public ArrayList<HoaDon> getByMaKhachHang(String maKhachHang) {
        //trạng thái đã thanh toán
        String sql = "SELECT * FROM HOADON WHERE maKhachHang=? AND trangThai=?";
        return get(sql, maKhachHang, String.valueOf(HoaDon.DA_THANH_TOAN));
    }

    // Sync a HoaDon object to Firebase
    private void syncToFirebase(HoaDon hoaDon) {
        Map<String, Object> hoaDonMap = SyncUtils.convertHoaDonToMap(hoaDon);

        dbRef.child(TABLE_NAME).child(String.valueOf(hoaDon.getMaHoaDon()))
                .setValue(hoaDonMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        logSuccessfulSync("Sync", String.valueOf(hoaDon.getMaHoaDon()));
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        handleSyncError("Sync", String.valueOf(hoaDon.getMaHoaDon()), e);
                    }
                });
    }

    // Delete a HoaDon from Firebase
    private void deleteFromFirebase(String maHoaDon) {
        dbRef.child(TABLE_NAME).child(maHoaDon)
                .removeValue()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        logSuccessfulSync("Delete", maHoaDon);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        handleSyncError("Delete", maHoaDon, e);
                    }
                });
    }

    /**
     * Interface để nhận callback khi lấy hóa đơn từ Firebase
     */
    public interface OnHoaDonListener {
        void onHoaDonReceived(HoaDon hoaDon);
        void onError(Exception e);
    }

    private ValueEventListener firebaseDirectListener;
    private OnHoaDonListener hoaDonListener;

    /**
     * Lấy hóa đơn trực tiếp từ Firebase (không sync về SQLite)
     * Dùng khi cần data mới nhất từ web admin
     * 
     * @param maBan Mã bàn
     * @param maKhachHang Mã khách hàng (có thể null cho nhân viên)
     * @param trangThai Trạng thái hóa đơn (0 = chưa thanh toán)
     * @param ngayGioSuDung Ngày giờ sử dụng từ DatBan (có thể null, format: "dd-MM-yyyy HH:mm:ss")
     * @param listener Callback để nhận kết quả
     */
   public void getByMaBanFromFirebaseDirect(String maBan, String maKhachHang, int trangThai, String ngayGioSuDung, OnHoaDonListener listener) {
    this.hoaDonListener = listener;
    
    // Remove listener cũ nếu có
    if (firebaseDirectListener != null) {
        dbRef.child(TABLE_NAME).removeEventListener(firebaseDirectListener);
    }
    
    // Tạo listener để lấy một lần
    firebaseDirectListener = new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
            HoaDon foundHoaDon = null;
            ArrayList<HoaDon> matchingHoaDon = new ArrayList<>();
            
            // ==========================================
            // FIX LỖI NumberFormatException Ở ĐÂY
            // ==========================================
            int targetMaBan = -1;
            if (maBan != null && !maBan.trim().isEmpty()) {
                try {
                    targetMaBan = Integer.parseInt(maBan.trim());
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Lỗi: maBan truyền vào không phải là số hợp lệ: " + maBan);
                    // Báo lỗi về UI và dừng
                    final String errorMsg = "Mã bàn không đúng định dạng số: " + maBan;
                    runOnUiThread(() -> {
                        if (hoaDonListener != null) hoaDonListener.onError(new Exception(errorMsg));
                    });
                    return; // Dừng hàm ngay lập tức
                }
            } else {
                Log.e(TAG, "Lỗi nghiêm trọng: maBan truyền vào bị null hoặc rỗng!");
                // Báo lỗi về UI và dừng vòng lặp
                runOnUiThread(() -> {
                    if (hoaDonListener != null) hoaDonListener.onError(new Exception("Mã bàn không được để trống!"));
                });
                
                // Dọn dẹp listener luôn
                if (firebaseDirectListener != null) {
                    dbRef.child(TABLE_NAME).removeEventListener(firebaseDirectListener);
                    firebaseDirectListener = null;
                }
                return; // Dừng hàm ngay lập tức, không chạy xuống dưới nữa
            }
            // ==========================================

            // Parse ngayGioSuDung nếu có (để so sánh ngày)
            java.util.Date targetDate = null;
            if (ngayGioSuDung != null && !ngayGioSuDung.isEmpty()) {
                try {
                    targetDate = XDate.toDateTime(ngayGioSuDung);
                    Log.d(TAG, "Đang tìm hóa đơn theo ngày: " + ngayGioSuDung);
                } catch (ParseException e) {
                    Log.w(TAG, "Không parse được ngayGioSuDung: " + ngayGioSuDung, e);
                }
            }
            
            Log.d(TAG, "Đang tìm hóa đơn: maBan=" + maBan + ", maKhachHang=" + maKhachHang + 
                  ", trangThai=" + trangThai + ", ngayGioSuDung=" + ngayGioSuDung);
            Log.d(TAG, "Tổng số children trong Firebase: " + dataSnapshot.getChildrenCount());
            
            // Đếm số hóa đơn đã xử lý
            int processedCount = 0;
            int nullCount = 0;
            int errorCount = 0;
            
            // Xử lý dữ liệu từ Firebase (có thể là array hoặc object)
            for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                String snapshotKey = snapshot.getKey();
                
                try {
                    // Bỏ qua giá trị null
                    if (snapshot.getValue() == null) {
                        nullCount++;
                        Log.d(TAG, "Bỏ qua hóa đơn null tại index: " + snapshotKey);
                        continue;
                    }
                    
                    Map<String, Object> map = (Map<String, Object>) snapshot.getValue();
                    if (map == null) {
                        nullCount++;
                        Log.d(TAG, "Bỏ qua hóa đơn null map tại index: " + snapshotKey);
                        continue;
                    }
                    
                    try {
                        processedCount++;
                        
                        // Parse thủ công để xử lý nhiều kiểu dữ liệu
                        HoaDon hoaDon = parseHoaDonFromMap(map);
                        
                        if (hoaDon == null) {
                            errorCount++;
                            Log.w(TAG, "Parse thất bại cho hóa đơn tại index: " + snapshotKey);
                            continue;
                        }
                            
                        String gioVaoStr = hoaDon.getGioVao() != null ? XDate.toStringDateTime(hoaDon.getGioVao()) : "null";
                        Log.d(TAG, "Kiểm tra hóa đơn: maHoaDon=" + hoaDon.getMaHoaDon() + 
                              ", maBan=" + hoaDon.getMaBan() + ", maKhachHang=" + hoaDon.getMaKhachHang() + 
                              ", trangThai=" + hoaDon.getTrangThai() + ", gioVao=" + gioVaoStr);
                        
                        // Kiểm tra điều kiện cơ bản
                        boolean maBanMatch = hoaDon.getMaBan() == targetMaBan;
                        boolean trangThaiMatch = hoaDon.getTrangThai() == trangThai;
                        boolean matches = maBanMatch && trangThaiMatch;
                        
                        Log.d(TAG, "So sánh maBan: " + hoaDon.getMaBan() + " == " + targetMaBan + " = " + maBanMatch);
                        Log.d(TAG, "So sánh trangThai: " + hoaDon.getTrangThai() + " == " + trangThai + " = " + trangThaiMatch);
                        
                        // Nếu có maKhachHang, kiểm tra thêm
                        if (maKhachHang != null && !maKhachHang.isEmpty()) {
                            boolean khachHangMatch = maKhachHang.equals(hoaDon.getMaKhachHang());
                            matches = matches && khachHangMatch;
                            Log.d(TAG, "So sánh maKhachHang: '" + maKhachHang + "' == '" + hoaDon.getMaKhachHang() + "' = " + khachHangMatch);
                        } else {
                            Log.d(TAG, "Bỏ qua kiểm tra maKhachHang (maKhachHang is null or empty)");
                        }
                        
                        // Nếu có ngayGioSuDung, so sánh ngày (chỉ so sánh phần ngày, không cần giờ chính xác)
                        if (targetDate != null && hoaDon.getGioVao() != null) {
                            // So sánh chỉ phần ngày (yyyy-MM-dd), bỏ qua giờ
                            try {
                                String targetDateStr = XDate.toStringDate(targetDate); // Format: "yyyy-MM-dd"
                                String hoaDonDateStr = XDate.toStringDate(hoaDon.getGioVao()); // Format: "yyyy-MM-dd"
                                boolean dateMatch = targetDateStr.equals(hoaDonDateStr);
                                matches = matches && dateMatch;
                                Log.d(TAG, "So sánh ngày: targetDate=" + targetDateStr + ", hoaDonDate=" + hoaDonDateStr + ", match=" + dateMatch);
                                Log.d(TAG, "  - targetDate raw: " + targetDate.toString());
                                Log.d(TAG, "  - hoaDon.getGioVao() raw: " + hoaDon.getGioVao().toString());
                            } catch (Exception e) {
                                Log.e(TAG, "Lỗi khi so sánh ngày", e);
                                Log.e(TAG, "  - targetDate: " + (targetDate != null ? targetDate.toString() : "null"));
                                Log.e(TAG, "  - hoaDon.getGioVao(): " + (hoaDon.getGioVao() != null ? hoaDon.getGioVao().toString() : "null"));
                                // Nếu lỗi parse ngày, bỏ qua điều kiện ngày (lấy hóa đơn mới nhất)
                            }
                        } else {
                            if (targetDate == null) {
                                Log.d(TAG, "Bỏ qua kiểm tra ngày (targetDate is null)");
                            }
                            if (hoaDon.getGioVao() == null) {
                                Log.d(TAG, "Bỏ qua kiểm tra ngày (hoaDon.getGioVao() is null)");
                            }
                        }
                        
                        Log.d(TAG, "Hóa đơn " + hoaDon.getMaHoaDon() + " FINAL matches: " + matches);
                        
                        if (matches) {
                            matchingHoaDon.add(hoaDon);
                            Log.d(TAG, "✓ Đã thêm hóa đơn " + hoaDon.getMaHoaDon() + " vào danh sách matching");
                        }
                    } catch (Exception e) {
                        errorCount++;
                        Log.e(TAG, "Lỗi khi xử lý hóa đơn tại index " + snapshotKey, e);
                        // KHÔNG break, tiếp tục với hóa đơn tiếp theo
                    }
                } catch (Exception e) {
                    errorCount++;
                    Log.e(TAG, "Lỗi nghiêm trọng khi xử lý snapshot " + snapshotKey, e);
                    // KHÔNG break, tiếp tục với snapshot tiếp theo
                }
            }
            
            Log.d(TAG, "=== KẾT QUẢ SCAN ===");
            Log.d(TAG, "- Tổng children: " + dataSnapshot.getChildrenCount());
            Log.d(TAG, "- Đã xử lý: " + processedCount);
            Log.d(TAG, "- Null/Empty: " + nullCount);
            Log.d(TAG, "- Lỗi: " + errorCount);
            Log.d(TAG, "- Matching: " + matchingHoaDon.size());
            
            // Sắp xếp theo maHoaDon DESC để lấy hóa đơn mới nhất
            matchingHoaDon.sort((h1, h2) -> Integer.compare(h2.getMaHoaDon(), h1.getMaHoaDon()));
            
            if (!matchingHoaDon.isEmpty()) {
                foundHoaDon = matchingHoaDon.get(0);
                Log.d(TAG, "Tìm thấy hóa đơn: " + foundHoaDon.getMaHoaDon());
            } else {
                Log.w(TAG, "Không tìm thấy hóa đơn nào matching với điều kiện");
            }
            
            // Callback về main thread
            final HoaDon finalHoaDon = foundHoaDon;
            runOnUiThread(() -> {
                if (hoaDonListener != null) {
                    if (finalHoaDon != null) {
                        hoaDonListener.onHoaDonReceived(finalHoaDon);
                    } else {
                        hoaDonListener.onError(new Exception("Không tìm thấy hóa đơn với maBan=" + maBan + 
                            (maKhachHang != null ? ", maKhachHang=" + maKhachHang : "") + ", trangThai=" + trangThai));
                    }
                }
            });
            
            // Remove listener sau khi lấy xong (chỉ lấy một lần)
            if (firebaseDirectListener != null) {
                dbRef.child(TABLE_NAME).removeEventListener(firebaseDirectListener);
                firebaseDirectListener = null;
            }
        }

        @Override
        public void onCancelled(@NonNull DatabaseError error) {
            Log.e(TAG, "Firebase listener cancelled", error.toException());
            runOnUiThread(() -> {
                if (hoaDonListener != null) {
                    hoaDonListener.onError(error.toException());
                }
            });
            
            // Remove listener
            if (firebaseDirectListener != null) {
                dbRef.child(TABLE_NAME).removeEventListener(firebaseDirectListener);
                firebaseDirectListener = null;
            }
        }
    };
    
    // Attach listener (chỉ lấy một lần)
    dbRef.child(TABLE_NAME).addListenerForSingleValueEvent(firebaseDirectListener);
}
    /**
     * Parse HoaDon từ Map với xử lý nhiều kiểu dữ liệu
     * Xử lý Long, Integer, Double từ Firebase
     * KHÔNG throw exception - trả về null nếu có lỗi để không break vòng lặp
     */
    private HoaDon parseHoaDonFromMap(Map<String, Object> map) {
        try {
            HoaDon hoaDon = new HoaDon();
            
            // Parse maHoaDon (có thể là Long, Integer, hoặc Double)
            Object maHoaDonObj = map.get("maHoaDon");
            if (maHoaDonObj != null) {
                try {
                    if (maHoaDonObj instanceof Long) {
                        hoaDon.setMaHoaDon(((Long) maHoaDonObj).intValue());
                    } else if (maHoaDonObj instanceof Integer) {
                        hoaDon.setMaHoaDon((Integer) maHoaDonObj);
                    } else if (maHoaDonObj instanceof Double) {
                        hoaDon.setMaHoaDon(((Double) maHoaDonObj).intValue());
                    } else {
                        Log.w(TAG, "maHoaDon type không hỗ trợ: " + maHoaDonObj.getClass().getName());
                        return null;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Lỗi parse maHoaDon", e);
                    return null;
                }
            } else {
                Log.w(TAG, "maHoaDon is null");
                return null;
            }
            
            // Parse maBan (có thể là Long, Integer, hoặc Double)
            Object maBanObj = map.get("maBan");
            if (maBanObj != null) {
                try {
                    if (maBanObj instanceof Long) {
                        hoaDon.setMaBan(((Long) maBanObj).intValue());
                    } else if (maBanObj instanceof Integer) {
                        hoaDon.setMaBan((Integer) maBanObj);
                    } else if (maBanObj instanceof Double) {
                        hoaDon.setMaBan(((Double) maBanObj).intValue());
                    } else {
                        Log.w(TAG, "maBan type không hỗ trợ: " + maBanObj.getClass().getName());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Lỗi parse maBan", e);
                }
            }
            
            // Parse trangThai (có thể là Long, Integer, hoặc Double)
            Object trangThaiObj = map.get("trangThai");
            if (trangThaiObj != null) {
                try {
                    if (trangThaiObj instanceof Long) {
                        hoaDon.setTrangThai(((Long) trangThaiObj).intValue());
                    } else if (trangThaiObj instanceof Integer) {
                        hoaDon.setTrangThai((Integer) trangThaiObj);
                    } else if (trangThaiObj instanceof Double) {
                        hoaDon.setTrangThai(((Double) trangThaiObj).intValue());
                    } else {
                        Log.w(TAG, "trangThai type không hỗ trợ: " + trangThaiObj.getClass().getName());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Lỗi parse trangThai", e);
                }
            }
            
            // Parse gioVao và gioRa
            try {
                Object gioVaoObj = map.get("gioVao");
                Object gioRaObj = map.get("gioRa");
                
                String gioVaoStr = null;
                String gioRaStr = null;
                
                // Xử lý gioVao có thể là String hoặc null
                if (gioVaoObj instanceof String) {
                    gioVaoStr = (String) gioVaoObj;
                } else if (gioVaoObj != null) {
                    gioVaoStr = gioVaoObj.toString();
                }
                
                // Xử lý gioRa có thể là String hoặc null
                if (gioRaObj instanceof String) {
                    gioRaStr = (String) gioRaObj;
                } else if (gioRaObj != null) {
                    gioRaStr = gioRaObj.toString();
                }
                
                if (gioVaoStr != null && !gioVaoStr.isEmpty()) {
                    try {
                        hoaDon.setGioVao(XDate.toDateTime(gioVaoStr));
                        // Log.d(TAG, "Parsed gioVao: " + gioVaoStr); // Commented để giảm log
                    } catch (ParseException e) {
                        Log.e(TAG, "Error parsing gioVao: " + gioVaoStr, e);
                        // Không return null, tiếp tục với các field khác
                    }
                }
                
                if (gioRaStr != null && !gioRaStr.isEmpty()) {
                    try {
                        hoaDon.setGioRa(XDate.toDateTime(gioRaStr));
                    } catch (ParseException e) {
                        Log.e(TAG, "Error parsing gioRa: " + gioRaStr, e);
                        // Không return null, tiếp tục với các field khác
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error parsing date from map", e);
                // Không return null, tiếp tục với các field khác
            }
            
            // Parse maKhachHang và ghiChu
            try {
                hoaDon.setMaKhachHang((String) map.get("maKhachHang"));
                hoaDon.setGhiChu((String) map.get("ghiChu"));
            } catch (Exception e) {
                Log.e(TAG, "Error parsing maKhachHang/ghiChu", e);
            }
            
            return hoaDon;
        } catch (Exception e) {
            Log.e(TAG, "CRITICAL: Error parsing HoaDon from map", e);
            Log.e(TAG, "Map content: " + map.toString());
            return null;
        }
    }

    /**
     * Dừng lắng nghe từ Firebase
     */
    public void stopFirebaseDirectListener() {
        if (firebaseDirectListener != null) {
            dbRef.child(TABLE_NAME).removeEventListener(firebaseDirectListener);
            firebaseDirectListener = null;
        }
        hoaDonListener = null;
    }
    public void updateHoaDonToFirebaseDirect(HoaDon hoaDon,
                                             OnSuccessListener<Void> onSuccess,
                                             OnFailureListener onFailure) {
        Map<String, Object> hoaDonMap = SyncUtils.convertHoaDonToMap(hoaDon);
        dbRef.child(TABLE_NAME)
                .child(String.valueOf(hoaDon.getMaHoaDon()))
                .setValue(hoaDonMap)
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }
}