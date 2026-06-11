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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import app.edu.app.database.CoffeeDB;
import app.edu.app.interfaces.RealtimeUpdateCallback;
import app.edu.app.utils.ImageCache;
import app.edu.app.utils.StorageUtils;
import app.edu.app.utils.XDate;
import app.edu.app.model.NguoiDung;
import app.edu.app.utils.SyncUtils;

public class NguoiDungDAO extends FirebaseDAO {
    private static final String TABLE_NAME = "NguoiDung";
    CoffeeDB coffeeDB;

    public NguoiDungDAO(Context context) {
        super(context);
        this.coffeeDB = new CoffeeDB(context);
    }

    @SuppressLint("Range")
    public ArrayList<NguoiDung> get(String sql, String... choose) {
        ArrayList<NguoiDung> list = new ArrayList<>();
        SQLiteDatabase sqLiteDatabase = coffeeDB.getReadableDatabase();
        @SuppressLint("Recycle") Cursor cursor = sqLiteDatabase.rawQuery(sql, choose);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            do {
                NguoiDung nguoiDung = new NguoiDung();
                nguoiDung.setMaNguoiDung(cursor.getString(cursor.getColumnIndex("maNguoiDung")));
                nguoiDung.setHoVaTen(cursor.getString(cursor.getColumnIndex("hoVaTen")));
                nguoiDung.setHinhAnh(cursor.getBlob(cursor.getColumnIndex("hinhAnh")));
                try {
                    nguoiDung.setNgaySinh(XDate.toDate(cursor.getString(cursor.getColumnIndex("ngaySinh"))));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                nguoiDung.setEmail(cursor.getString(cursor.getColumnIndex("email")));
                nguoiDung.setChucVu(cursor.getString(cursor.getColumnIndex("chucVu")));
                nguoiDung.setGioiTinh(cursor.getString(cursor.getColumnIndex("gioiTinh")));
                nguoiDung.setMatKhau(cursor.getString(cursor.getColumnIndex("matKhau")));

                list.add(nguoiDung);
                // Removed verbose logging to improve performance
            } while (cursor.moveToNext());
        }

        return list;
    }

    /**
     * Get all users from local database only (non-blocking)
     */
    public ArrayList<NguoiDung> getAllLocal() {
        String sqlGetAll = "SELECT * FROM NGUOIDUNG";
        return get(sqlGetAll);
    }

    public ArrayList<NguoiDung> getAll() {
        // Return local data immediately
        ArrayList<NguoiDung> list = getAllLocal();

        // Fetch updates from Firebase in background (non-blocking)
        if (isNetworkAvailable()) {
            fetchFromFirebaseAsync();
        }

        return list;
    }

    // Fetch data from Firebase (deprecated - use fetchFromFirebaseAsync instead)
    private void fetchFromFirebase() {
        // Use async version to avoid blocking UI
        fetchFromFirebaseAsync();
    }

    // Add this at class level
    private RealtimeUpdateCallback nguoiDungCallback;

    /**
     * Start real-time synchronization for NguoiDung table
     */
    public void startRealtimeSync() {
        nguoiDungCallback = new RealtimeUpdateCallback() {
            @Override
            public void onDataChanged(DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    try {
                        Map<String, Object> map = (Map<String, Object>) snapshot.getValue();
                        if (map != null) {
                            NguoiDung firebaseNguoiDung = SyncUtils.convertMapToNguoiDung(map);

                            try {
                                NguoiDung localNguoiDung = getByMaNguoiDung(firebaseNguoiDung.getMaNguoiDung());
                                // Update if necessary
                                updateNguoiDungFromFirebase(firebaseNguoiDung);
                            } catch (Exception e) {
                                // User doesn't exist locally, insert them
                                insertNguoiDungFromFirebase(firebaseNguoiDung);
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing Firebase NguoiDung update", e);
                    }
                }
            }
        };

        setupRealtimeListener(TABLE_NAME, nguoiDungCallback);
    }

    /**
     * Stop real-time synchronization
     */
    public void stopRealtimeSync() {
        removeRealtimeListener(TABLE_NAME);
    }

    // Firebase update methods
    private boolean updateNguoiDungFromFirebase(NguoiDung nguoiDung) {
        SQLiteDatabase sqLiteDatabase = coffeeDB.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("hoVaTen", nguoiDung.getHoVaTen());
        values.put("hinhAnh", nguoiDung.getHinhAnh());
        values.put("ngaySinh", XDate.toStringDate(nguoiDung.getNgaySinh()));
        values.put("email", nguoiDung.getEmail());
        values.put("chucVu", nguoiDung.getChucVu());
        values.put("gioiTinh", nguoiDung.getGioiTinh());
        values.put("matKhau", nguoiDung.getMatKhau());

        int check = sqLiteDatabase.update("NGUOIDUNG", values, "maNguoiDung=?",
                new String[]{String.valueOf(nguoiDung.getMaNguoiDung())});

        if (check > 0) {
            isLocalUpdate = true;
            Log.d(TAG, "Updated NguoiDung from Firebase: " + nguoiDung.getMaNguoiDung());
        }
        isLocalUpdate = false;
        return check > 0;
    }

    private boolean insertNguoiDungFromFirebase(NguoiDung nguoiDung) {
        SQLiteDatabase sqLiteDatabase = coffeeDB.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("maNguoiDung", nguoiDung.getMaNguoiDung());
        values.put("hoVaTen", nguoiDung.getHoVaTen());
        values.put("hinhAnh", nguoiDung.getHinhAnh());
        values.put("ngaySinh", XDate.toStringDate(nguoiDung.getNgaySinh()));
        values.put("email", nguoiDung.getEmail());
        values.put("chucVu", nguoiDung.getChucVu());
        values.put("gioiTinh", nguoiDung.getGioiTinh());
        values.put("matKhau", nguoiDung.getMatKhau());

        long check = sqLiteDatabase.insert("NGUOIDUNG", null, values);

        if (check != -1) {
            Log.d(TAG, "Inserted NguoiDung from Firebase: " + nguoiDung.getMaNguoiDung());
        }

        return check != -1;
    }


    public boolean insertNguoiDung(NguoiDung nguoiDung) {
        SQLiteDatabase sqLiteDatabase = coffeeDB.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("maNguoiDung", nguoiDung.getMaNguoiDung());
        values.put("hoVaTen", nguoiDung.getHoVaTen());
        values.put("hinhAnh", nguoiDung.getHinhAnh());
        values.put("ngaySinh", XDate.toStringDate(nguoiDung.getNgaySinh()));
        values.put("email", nguoiDung.getEmail());
        values.put("chucVu", nguoiDung.getChucVu());
        values.put("gioiTinh", nguoiDung.getGioiTinh());
        values.put("matKhau", nguoiDung.getMatKhau());
        long check = sqLiteDatabase.insert("NGUOIDUNG", null, values);

        // If local insert successful and network available, sync to Firebase
        if (check != -1 && isNetworkAvailable()) {
            isLocalUpdate = true;
            syncToFirebase(nguoiDung);
        }
        isLocalUpdate = false;

        return check != -1;
    }

    public boolean updateNguoiDung(NguoiDung nguoiDung) {
        SQLiteDatabase sqLiteDatabase = coffeeDB.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("hoVaTen", nguoiDung.getHoVaTen());
        values.put("hinhAnh", nguoiDung.getHinhAnh());
        values.put("ngaySinh", XDate.toStringDate(nguoiDung.getNgaySinh()));
        values.put("email", nguoiDung.getEmail());
        values.put("chucVu", nguoiDung.getChucVu());
        values.put("gioiTinh", nguoiDung.getGioiTinh());
        values.put("matKhau", nguoiDung.getMatKhau());
        int check = sqLiteDatabase.update("NGUOIDUNG", values, "maNguoiDung=?", new String[]{String.valueOf(nguoiDung.getMaNguoiDung())});

        // If local update successful and network available, sync to Firebase
        if (check > 0 && isNetworkAvailable()) {
            isLocalUpdate = true;
            syncToFirebase(nguoiDung);
        }
        isLocalUpdate = false;

        return check > 0;
    }

    public boolean deleteNguoiDung(String maNguoiDung) {
        SQLiteDatabase sqLiteDatabase = coffeeDB.getWritableDatabase();
        int check = sqLiteDatabase.delete("NGUOIDUNG", "maNguoiDung=?", new String[]{maNguoiDung});

        // If local delete successful and network available, delete from Firebase
        if (check > 0 && isNetworkAvailable()) {
            deleteFromFirebase(maNguoiDung);

            String imageUrl = getImageUrlFromFirebase(maNguoiDung);
            if (imageUrl != null) {
                StorageUtils.deleteImage(imageUrl);
            }
        }

        return check > 0;
    }

    private String getImageUrlFromFirebase(String maLoai) {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<String> imageUrl = new AtomicReference<>(null);

        dbRef.child("LoaiHang").child(maLoai).child("hinhAnh")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        imageUrl.set(snapshot.getValue(String.class));
                        latch.countDown();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        latch.countDown();
                    }
                });

        try {
            latch.await();
        } catch (InterruptedException e) {
            Log.e(TAG, "Interrupted while getting image URL", e);
        }

        return imageUrl.get();
    }

    public NguoiDung getByMaNguoiDung(String maNguoiDung) {
        String sqlGetMaNguoiDung = "SELECT * FROM NGUOIDUNG WHERE maNguoiDung=?";
        ArrayList<NguoiDung> list = get(sqlGetMaNguoiDung, maNguoiDung);

        if (list.isEmpty()) {
            return null;
        }
        return list.get(0);
    }
    
    /**
     * Lấy người dùng từ local database chỉ (không sync Firebase)
     * Dùng cho các tác vụ cần response nhanh như login
     */
    public NguoiDung getByMaNguoiDungLocal(String maNguoiDung) {
        String sqlGetMaNguoiDung = "SELECT * FROM NGUOIDUNG WHERE maNguoiDung=?";
        ArrayList<NguoiDung> list = get(sqlGetMaNguoiDung, maNguoiDung);

        if (list.isEmpty()) {
            return null;
        }
        return list.get(0);
    }

    public ArrayList<NguoiDung> getAllPositionNhanVien() {
        String sqlGetMaNguoiDung = "SELECT * FROM NGUOIDUNG WHERE chucVu=?";

        return get(sqlGetMaNguoiDung, NguoiDung.POSITION_STAFF);
    }

    /**
     * Kiểm tra đăng nhập từ local database chỉ (không sync Firebase)
     * Dùng cho login để tránh block UI
     */
    public boolean checkLogin(String tenDangNhap, String matKhau) {
        String sqlCheckLogin = "SELECT * FROM NGUOIDUNG WHERE maNguoiDung=? AND matKhau=?";
        ArrayList<NguoiDung> list = get(sqlCheckLogin, tenDangNhap, matKhau);

        // Trigger sync in background (không block login)
        if (isNetworkAvailable()) {
            fetchFromFirebaseAsync();
        }

        return list.size() != 0;
    }
    
    /**
     * Fetch data from Firebase in background (non-blocking)
     */
    private void fetchFromFirebaseAsync() {
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
                
                // Process all data in background thread to avoid blocking UI
                runInBackground(() -> {
                    SQLiteDatabase sqLiteDatabase = coffeeDB.getWritableDatabase();
                    sqLiteDatabase.beginTransaction();
                    try {
                        for (Map<String, Object> map : dataList) {
                            try {
                                NguoiDung firebaseNguoiDung = SyncUtils.convertMapToNguoiDung(map);
                                
                                // Check if user exists locally
                                String sqlCheck = "SELECT * FROM NGUOIDUNG WHERE maNguoiDung=?";
                                Cursor cursor = sqLiteDatabase.rawQuery(sqlCheck, new String[]{firebaseNguoiDung.getMaNguoiDung()});
                                
                                ContentValues values = new ContentValues();
                                values.put("hoVaTen", firebaseNguoiDung.getHoVaTen());
                                values.put("email", firebaseNguoiDung.getEmail());
                                values.put("chucVu", firebaseNguoiDung.getChucVu());
                                values.put("gioiTinh", firebaseNguoiDung.getGioiTinh());
                                values.put("matKhau", firebaseNguoiDung.getMatKhau());
                                if (firebaseNguoiDung.getNgaySinh() != null) {
                                    values.put("ngaySinh", XDate.toStringDate(firebaseNguoiDung.getNgaySinh()));
                                }
                                
                                if (cursor.getCount() > 0) {
                                    // Update existing
                                    sqLiteDatabase.update("NGUOIDUNG", values, "maNguoiDung=?",
                                            new String[]{firebaseNguoiDung.getMaNguoiDung()});
                                } else {
                                    // Insert new
                                    values.put("maNguoiDung", firebaseNguoiDung.getMaNguoiDung());
                                    sqLiteDatabase.insert("NGUOIDUNG", null, values);
                                }
                                cursor.close();
                            } catch (Exception e) {
                                Log.e(TAG, "Error processing NguoiDung data", e);
                            }
                        }
                        sqLiteDatabase.setTransactionSuccessful();
                    } finally {
                        sqLiteDatabase.endTransaction();
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Firebase fetch cancelled", error.toException());
            }
        });
    }

    //check email by email
    public boolean checkEmail(String email) {
        String sqlCheckEmail = "SELECT * FROM NGUOIDUNG WHERE email=?";
        ArrayList<NguoiDung> list = get(sqlCheckEmail, email);
        return list.size() != 0;
    }

    public NguoiDung getByEmail(String email) {
        String sqlGetMaNguoiDung = "SELECT * FROM NGUOIDUNG WHERE email=?";
        ArrayList<NguoiDung> list = get(sqlGetMaNguoiDung, email);

        return list.get(0);
    }

    // Sync a NguoiDung object to Firebase
    private void syncToFirebase(NguoiDung nguoiDung) {
        Map<String, Object> nguoiDungMap = SyncUtils.convertNguoiDungToMap(nguoiDung);

        dbRef.child(TABLE_NAME).child(nguoiDung.getMaNguoiDung())
                .setValue(nguoiDungMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        logSuccessfulSync("Sync", nguoiDung.getMaNguoiDung());

                        if (nguoiDung.getHinhAnh() != null && nguoiDung.getHinhAnh().length > 0) {
                            uploadImageToStorage(nguoiDung);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        handleSyncError("Sync", nguoiDung.getMaNguoiDung(), e);
                    }
                });
    }

    private void uploadImageToStorage(NguoiDung nguoiDung) {
        if (nguoiDung.getHinhAnh() != null || nguoiDung.getHinhAnh().length > 0) {
            String path = "nguoidung/" + nguoiDung.getMaNguoiDung();
            StorageUtils.uploadImage( nguoiDung.getHinhAnh(),path, new StorageUtils.OnUploadCompleteListener() {
                @Override
                public void onSuccess(String downloadUrl) {
                    // Update the download URL in Firebase
                    dbRef.child(TABLE_NAME).child(nguoiDung.getMaNguoiDung()).child("hinhAnh")
                            .setValue(downloadUrl)
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    logSuccessfulSync("Image upload", nguoiDung.getMaNguoiDung());
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    handleSyncError("Image upload", nguoiDung.getMaNguoiDung(), e);
                                }
                            });

                    ImageCache.addUrlToCache("nguoidung_" + nguoiDung.getMaNguoiDung(), downloadUrl);
                }

                @Override
                public void onFailure(Exception e) {

                }
            });
        }
    }

    /**
     * Download image from URL and update local database
     */
    private void downloadImageFromUrl(NguoiDung nguoiDung, String imageUrl) {
        StorageUtils.downloadImage(imageUrl, new StorageUtils.OnDownloadCompleteListener() {
            @Override
            public void onSuccess(byte[] imageData) {
                // Update the model object
                nguoiDung.setHinhAnh(imageData);

                // Update SQLite database
                SQLiteDatabase db = coffeeDB.getWritableDatabase();
                ContentValues values = new ContentValues();
                values.put("hinhAnh", imageData);
                db.update("NGUOIDUNG", values, "maNguoiDung=?",
                        new String[]{nguoiDung.getMaNguoiDung()});

                Log.d(TAG, "Image downloaded for NguoiDung: " + nguoiDung.getMaNguoiDung());
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Failed to download image for NguoiDung: " + nguoiDung.getMaNguoiDung(), e);
            }
        });
    }

    // Modify the existing syncToFirebase method


    // Delete a NguoiDung from Firebase
    private void deleteFromFirebase(String maNguoiDung) {
        dbRef.child(TABLE_NAME).child(maNguoiDung)
                .removeValue()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        logSuccessfulSync("Delete", maNguoiDung);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        handleSyncError("Delete", maNguoiDung, e);
                    }
                });
    }
    
    // ===================== FIREBASE DIRECT MODE =====================
    
    /**
     * Interface callback cho Firebase Direct mode
     */
    public interface OnNguoiDungListener {
        void onNguoiDungReceived(NguoiDung nguoiDung);
        void onError(Exception e);
    }
    
    /**
     * Lấy người dùng TRỰC TIẾP từ Firebase (không qua SQLite)
     * 
     * @param maNguoiDung Mã người dùng cần lấy
     * @param listener Callback nhận kết quả
     */
    public void getByMaNguoiDungFromFirebaseDirect(String maNguoiDung, OnNguoiDungListener listener) {
        dbRef.child(TABLE_NAME).child(maNguoiDung)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        try {
                            if (snapshot.exists()) {
                                Map<String, Object> map = (Map<String, Object>) snapshot.getValue();
                                if (map != null) {
                                    NguoiDung nguoiDung = SyncUtils.convertMapToNguoiDung(map);
                                    Log.d(TAG, "✓ Load người dùng từ Firebase: " + nguoiDung.getMaNguoiDung());
                                    listener.onNguoiDungReceived(nguoiDung);
                                } else {
                                    listener.onError(new Exception("Dữ liệu người dùng null"));
                                }
                            } else {
                                listener.onError(new Exception("Không tìm thấy người dùng: " + maNguoiDung));
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Lỗi parse người dùng từ Firebase", e);
                            listener.onError(e);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Firebase bị hủy khi load người dùng", error.toException());
                        listener.onError(error.toException());
                    }
                });
    }
}