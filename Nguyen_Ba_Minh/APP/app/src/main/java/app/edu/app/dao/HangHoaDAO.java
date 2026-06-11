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
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Map;

import app.edu.app.database.CoffeeDB;
import app.edu.app.interfaces.OnUrlFetchedListener;
import app.edu.app.interfaces.RealtimeUpdateCallback;
import app.edu.app.model.HangHoa;
import app.edu.app.utils.ImageCache;
import app.edu.app.utils.StorageUtils;
import app.edu.app.utils.SyncUtils;

public class HangHoaDAO extends FirebaseDAO {
    private static final String TABLE_NAME = "HangHoa";
    private static final String TAG = "HangHoaDAO";
    private CoffeeDB coffeeDB;
    private boolean isSyncing = false;

    public HangHoaDAO(Context context) {
        super(context);
        this.coffeeDB = new CoffeeDB(context);
        // Initialize image cache
        ImageCache.initialize(context);
        
        // Enable keepSynced để Firebase tự động sync node HangHoa
        try {
            dbRef.child(TABLE_NAME).keepSynced(true);
            Log.d(TAG, "✓ Enabled keepSynced for HangHoa node");
        } catch (Exception e) {
            Log.e(TAG, "Failed to enable keepSynced", e);
        }
    }

    @SuppressLint("Range")
    public ArrayList<HangHoa> get(String sql, String... choose) {
        ArrayList<HangHoa> list = new ArrayList<>();
        SQLiteDatabase sqLiteDatabase = coffeeDB.getWritableDatabase();
        @SuppressLint("Recycle") Cursor cursor = sqLiteDatabase.rawQuery(sql, choose);
        if (cursor.getCount() != 0) {
            cursor.moveToFirst();
            do {
                HangHoa hangHoa = new HangHoa();
                hangHoa.setMaHangHoa(cursor.getInt(cursor.getColumnIndex("maHangHoa")));
                hangHoa.setTenHangHoa(cursor.getString(cursor.getColumnIndex("tenHangHoa")));
                hangHoa.setHinhAnh(cursor.getBlob(cursor.getColumnIndex("hinhAnh")));
                hangHoa.setGiaTien(cursor.getInt(cursor.getColumnIndex("giaTien")));
                hangHoa.setMaLoai(cursor.getInt(cursor.getColumnIndex("maLoai")));
                hangHoa.setTrangThai(cursor.getInt(cursor.getColumnIndex("trangThai")));

                // Add to image cache if image exists
                if (hangHoa.getHinhAnh() != null) {
                    ImageCache.addBitmapToMemoryCache(String.valueOf(hangHoa.getMaHangHoa()), hangHoa.getHinhAnh());
                }

                list.add(hangHoa);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    public ArrayList<HangHoa> getAll() {
        String sqlGetAll = "SELECT * FROM HANGHOA";
        ArrayList<HangHoa> list = get(sqlGetAll);

        // Start fetch in background if needed and not already syncing
        if (isNetworkAvailable() && shouldSync(TABLE_NAME) && !isSyncing) {
            runInBackground(() -> fetchFromFirebase(null));
        }

        return list;
    }

    // Get data by pages (useful for RecyclerView with many items)
    public ArrayList<HangHoa> getPage(int pageNumber, int pageSize) {
        int offset = pageNumber * pageSize;
        String sql = "SELECT * FROM HANGHOA LIMIT ? OFFSET ?";
        return get(sql, String.valueOf(pageSize), String.valueOf(offset));
    }

    // Fetch data from Firebase with callback
    public void fetchFromFirebase(final OperationCallback callback) {
        if (isSyncing) {
            if (callback != null) callback.onFailure(new Exception("Already syncing"));
            return;
        }

        isSyncing = true;

        // Get last sync time for incremental updates
        long lastSyncTime = syncPreferences.getHangHoaLastSyncTime();
        Query query = dbRef.child(TABLE_NAME);

        // If we have last sync time, only get updated items
        if (lastSyncTime > 0) {
            query = query.orderByChild("lastModified").startAt(lastSyncTime);
        }

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                runInBackground(() -> {
                    SQLiteDatabase db = coffeeDB.getWritableDatabase();
                    db.beginTransaction();
                    try {
                        // Process in batches for better performance
                        int count = 0;
                        final int BATCH_SIZE = 10;

                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            try {
                                Map<String, Object> map = (Map<String, Object>) snapshot.getValue();
                                if (map != null) {
                                    HangHoa firebaseHangHoa = SyncUtils.convertMapToHangHoa(map);

                                    // Check if item exists locally
                                    Cursor cursor = db.query("HANGHOA",
                                            new String[]{"maHangHoa"},
                                            "maHangHoa=?",
                                            new String[]{String.valueOf(firebaseHangHoa.getMaHangHoa())},
                                            null, null, null);

                                    boolean exists = cursor.getCount() > 0;
                                    cursor.close();

                                    ContentValues values = new ContentValues();
                                    values.put("tenHangHoa", firebaseHangHoa.getTenHangHoa());
                                    values.put("hinhAnh", firebaseHangHoa.getHinhAnh());
                                    values.put("giaTien", firebaseHangHoa.getGiaTien());
                                    values.put("maLoai", firebaseHangHoa.getMaLoai());
                                    values.put("trangThai", firebaseHangHoa.getTrangThai());

                                    if (exists) {
                                        db.update("HANGHOA", values, "maHangHoa=?",
                                                new String[]{String.valueOf(firebaseHangHoa.getMaHangHoa())});
                                    } else {
                                        values.put("maHangHoa", firebaseHangHoa.getMaHangHoa());
                                        db.insert("HANGHOA", null, values);
                                    }

                                    count++;
                                    if (count % BATCH_SIZE == 0) {
                                        // Commit batch to avoid large transactions
                                        db.setTransactionSuccessful();
                                        db.endTransaction();
                                        db.beginTransaction();
                                    }
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error processing Firebase data", e);
                            }
                        }

                        db.setTransactionSuccessful();
                        syncPreferences.updateTableSyncTimestamp(TABLE_NAME);

                        runOnUiThread(() -> {
                            isSyncing = false;
                            if (callback != null) callback.onSuccess();
                        });
                    } catch (Exception e) {
                        Log.e(TAG, "Error during Firebase sync", e);
                        runOnUiThread(() -> {
                            isSyncing = false;
                            if (callback != null) callback.onFailure(e);
                        });
                    } finally {
                        db.endTransaction();
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Firebase fetch cancelled", error.toException());
                isSyncing = false;
                if (callback != null) {
                    runOnUiThread(() -> callback.onFailure(error.toException()));
                }
            }
        });
    }

    // get all hang hoa từ A đến Z
    public ArrayList<HangHoa> getAllAtoZ() {
        String sqlGetAll = "SELECT * FROM HANGHOA ORDER BY tenHangHoa ASC";
        return get(sqlGetAll);
    }

    // get all hang hoa từ Z đến A
    public ArrayList<HangHoa> getAllZtoA() {
        String sqlGetAll = "SELECT * FROM HANGHOA ORDER BY tenHangHoa DESC";
        return get(sqlGetAll);
    }

    private RealtimeUpdateCallback hangHoaCallback;

    /**
     * Start real-time synchronization for HangHoa table
     */
    public void startRealtimeSync() {
        hangHoaCallback = new RealtimeUpdateCallback() {
            @Override
            public void onDataChanged(DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    try {
                        Map<String, Object> map = (Map<String, Object>) snapshot.getValue();
                        if (map != null) {
                            HangHoa firebaseHangHoa = SyncUtils.convertMapToHangHoa(map);

                            try {
                                HangHoa localHangHoa = getByMaHangHoa(String.valueOf(firebaseHangHoa.getMaHangHoa()));
                                // Update if there are differences
                                updateHangHoaFromFirebase(firebaseHangHoa);
                            } catch (Exception e) {
                                // Item doesn't exist locally, insert it
                                insertHangHoaFromFirebase(firebaseHangHoa);
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing Firebase HangHoa update", e);
                    }
                }
            }
        };

        setupRealtimeListener(TABLE_NAME, hangHoaCallback);
    }

    /**
     * Stop real-time synchronization
     */
    public void stopRealtimeSync() {
        removeRealtimeListener(TABLE_NAME);
    }

    // Special methods to update from Firebase without triggering sync loop
    private boolean updateHangHoaFromFirebase(HangHoa hangHoa) {
        SQLiteDatabase sqLiteDatabase = coffeeDB.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("tenHangHoa", hangHoa.getTenHangHoa());
        values.put("hinhAnh", hangHoa.getHinhAnh());
        values.put("giaTien", hangHoa.getGiaTien());
        values.put("maLoai", hangHoa.getMaLoai());
        values.put("trangThai", hangHoa.getTrangThai());

        int check = sqLiteDatabase.update("HangHoa", values, "maHangHoa=?",
                new String[]{String.valueOf(hangHoa.getMaHangHoa())});

        if (check > 0) {
            // Update image cache
            if (hangHoa.getHinhAnh() != null) {
                ImageCache.addBitmapToMemoryCache(String.valueOf(hangHoa.getMaHangHoa()), hangHoa.getHinhAnh());
            }
            Log.d(TAG, "Updated HangHoa from Firebase: " + hangHoa.getMaHangHoa());
        }

        return check > 0;
    }

    private boolean insertHangHoaFromFirebase(HangHoa hangHoa) {
        SQLiteDatabase sqLiteDatabase = coffeeDB.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("maHangHoa", hangHoa.getMaHangHoa());
        values.put("tenHangHoa", hangHoa.getTenHangHoa());
        values.put("hinhAnh", hangHoa.getHinhAnh());
        values.put("giaTien", hangHoa.getGiaTien());
        values.put("maLoai", hangHoa.getMaLoai());
        values.put("trangThai", hangHoa.getTrangThai());

        long check = sqLiteDatabase.insert("HangHoa", null, values);

        if (check != -1) {
            // Add to image cache
            if (hangHoa.getHinhAnh() != null) {
                ImageCache.addBitmapToMemoryCache(String.valueOf(hangHoa.getMaHangHoa()), hangHoa.getHinhAnh());
            }
            Log.d(TAG, "Inserted HangHoa from Firebase: " + hangHoa.getMaHangHoa());
        }

        return check != -1;
    }

    public boolean insertHangHoa(HangHoa hangHoa) {
        SQLiteDatabase sqLiteDatabase = coffeeDB.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("tenHangHoa", hangHoa.getTenHangHoa());
        values.put("hinhAnh", hangHoa.getHinhAnh());
        values.put("giaTien", hangHoa.getGiaTien());
        values.put("maLoai", hangHoa.getMaLoai());
        values.put("trangThai", hangHoa.getTrangThai());
        long check = sqLiteDatabase.insert("HangHoa", null, values);

        // If local insert successful, sync to Firebase in background
        if (check != -1) {
            // Get the auto-generated ID if not set
            if (hangHoa.getMaHangHoa() == 0) {
                Cursor cursor = sqLiteDatabase.rawQuery("SELECT last_insert_rowid() as lastId", null);
                if (cursor.moveToFirst()) {
                    @SuppressLint("Range") int lastId = cursor.getInt(cursor.getColumnIndex("lastId"));
                    hangHoa.setMaHangHoa(lastId);
                }
                cursor.close();
            }

            // Add to image cache
            if (hangHoa.getHinhAnh() != null) {
                ImageCache.addBitmapToMemoryCache(String.valueOf(hangHoa.getMaHangHoa()), hangHoa.getHinhAnh());
            }
            // Sync in background
            final HangHoa finalHangHoa = hangHoa;
            if (isNetworkAvailable()) {
                isLocalUpdate = true;
                runInBackground(() -> syncToFirebase(finalHangHoa));
            }

        }
        isLocalUpdate = false;

        return check != -1;
    }

    public boolean updateHangHoa(HangHoa hangHoa) {
        SQLiteDatabase sqLiteDatabase = coffeeDB.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("tenHangHoa", hangHoa.getTenHangHoa());
        values.put("hinhAnh", hangHoa.getHinhAnh());
        values.put("giaTien", hangHoa.getGiaTien());
        values.put("maLoai", hangHoa.getMaLoai());
        values.put("trangThai", hangHoa.getTrangThai());
        int check = sqLiteDatabase.update("HangHoa", values, "maHangHoa=?", new String[]{String.valueOf(hangHoa.getMaHangHoa())});

        // If local update successful, sync to Firebase in background
        if (check > 0) {
            // Update image cache
            if (hangHoa.getHinhAnh() != null) {
                ImageCache.addBitmapToMemoryCache(String.valueOf(hangHoa.getMaHangHoa()), hangHoa.getHinhAnh());
            }

            // Sync in background
            if (isNetworkAvailable()) {
                isLocalUpdate = true;
                runInBackground(() -> syncToFirebase(hangHoa));
            }
        }

        isLocalUpdate = false;

        return check > 0;
    }

    public boolean deleteHangHoa(String maHangHoa) {
        SQLiteDatabase sqLiteDatabase = coffeeDB.getWritableDatabase();

        // Check references before delete
        Cursor cursor = sqLiteDatabase.rawQuery("SELECT * FROM HOADONCHITIET WHERE maHangHoa=?", new String[]{maHangHoa});
        boolean hasReferences = cursor.getCount() > 0;
        cursor.close();

        if (hasReferences) {
            return false;
        }

        // Delete from local database
        int check = sqLiteDatabase.delete("HangHoa", "maHangHoa=?", new String[]{maHangHoa});

        // If local delete successful, delete from Firebase in background
        if (check > 0) {
            // Remove from image cache
            ImageCache.removeBitmapFromMemoryCache(maHangHoa);

            // Delete from Firebase in background
            if (isNetworkAvailable()) {
                isLocalUpdate = true;
                runInBackground(() -> deleteFromFirebase(maHangHoa));
            }
        }
        isLocalUpdate = false;

        return check > 0;
    }

    public HangHoa getByMaHangHoa(String maHangHoa) {
        String sqlGetByMaHangHoa = "SELECT * FROM HANGHOA WHERE maHangHoa=?";
        ArrayList<HangHoa> list = get(sqlGetByMaHangHoa, maHangHoa);
        if (list.isEmpty()) {
            return null;
        }
        return list.get(0);
    }

    public ArrayList<HangHoa> getByMaLoai(String maLoaiHang) {
        String sqlGetByMaHangHoa = "SELECT * FROM HANGHOA WHERE maloai=?";
        return get(sqlGetByMaHangHoa, maLoaiHang);
    }

    public ArrayList<HangHoa> getByTrangThai(String trangThai) {
        String sqlGetByMaHangHoa = "SELECT * FROM HANGHOA WHERE trangThai=?";
        return get(sqlGetByMaHangHoa, trangThai);
    }

    public HangHoa getHangHoaById(int maHangHoa) {
        String sqlGetByMaHangHoa = "SELECT * FROM HANGHOA WHERE maHangHoa=?";
        ArrayList<HangHoa> list = get(sqlGetByMaHangHoa, String.valueOf(maHangHoa));
        if (list.isEmpty()) {
            return null;
        }
        return list.get(0);
    }

    // Sync a HangHoa object to Firebase
    private void syncToFirebase(HangHoa hangHoa) {
        Map<String, Object> hangHoaMap = SyncUtils.convertHangHoaToMap(hangHoa);

        // Add timestamp for conflict resolution
        hangHoaMap.put("lastModified", System.currentTimeMillis());

        dbRef.child(TABLE_NAME).child(String.valueOf(hangHoa.getMaHangHoa()))
                .setValue(hangHoaMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        logSuccessfulSync("Sync", String.valueOf(hangHoa.getMaHangHoa()));

                        // Upload image separately after data is saved
                        if (hangHoa.getHinhAnh() != null && hangHoa.getHinhAnh().length > 0) {
                            uploadImageToStorage(hangHoa);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        handleSyncError("Sync", String.valueOf(hangHoa.getMaHangHoa()), e);
                    }
                });
    }

    // Delete a HangHoa from Firebase
    private void deleteFromFirebase(String maHangHoa) {
        // First get the image URL to delete it from storage
        dbRef.child(TABLE_NAME).child(maHangHoa).child("hinhAnhUrl")
                .get()
                .addOnSuccessListener(dataSnapshot -> {
                    String imageUrl = dataSnapshot.getValue(String.class);
                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        StorageUtils.deleteImage(imageUrl);
                    }

                    // Delete the database entry
                    dbRef.child(TABLE_NAME).child(maHangHoa)
                            .removeValue()
                            .addOnSuccessListener(aVoid -> logSuccessfulSync("Delete", maHangHoa))
                            .addOnFailureListener(e -> handleSyncError("Delete", maHangHoa, e));
                })
                .addOnFailureListener(e -> {
                    // If we can't get the image URL, still delete the database entry
                    dbRef.child(TABLE_NAME).child(maHangHoa)
                            .removeValue()
                            .addOnSuccessListener(aVoid -> logSuccessfulSync("Delete", maHangHoa))
                            .addOnFailureListener(error -> handleSyncError("Delete", maHangHoa, error));
                });
    }

    // Batch operations for better performance
    public boolean insertMultiple(ArrayList<HangHoa> hangHoaList) {
        SQLiteDatabase db = coffeeDB.getWritableDatabase();
        db.beginTransaction();

        try {
            for (HangHoa hangHoa : hangHoaList) {
                ContentValues values = new ContentValues();
                values.put("tenHangHoa", hangHoa.getTenHangHoa());
                values.put("hinhAnh", hangHoa.getHinhAnh());
                values.put("giaTien", hangHoa.getGiaTien());
                values.put("maLoai", hangHoa.getMaLoai());
                values.put("trangThai", hangHoa.getTrangThai());

                db.insert("HangHoa", null, values);
            }

            db.setTransactionSuccessful();

            // Sync in background
            if (isNetworkAvailable()) {
                runInBackground(() -> {
                    for (HangHoa hangHoa : hangHoaList) {
                        syncToFirebase(hangHoa);
                    }
                });
            }

            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error in batch insert", e);
            return false;
        } finally {
            db.endTransaction();
        }
    }

    // Add these methods to HangHoaDAO.java

    /**
     * Upload image to Firebase Storage
     */
    private void uploadImageToStorage(HangHoa hangHoa) {
        if (hangHoa.getHinhAnh() != null && hangHoa.getHinhAnh().length > 0) {
            String path = "hanghoa/" + hangHoa.getMaHangHoa() + ".jpg";

            StorageUtils.uploadImage(hangHoa.getHinhAnh(), path, new StorageUtils.OnUploadCompleteListener() {
                @Override
                public void onSuccess(String downloadUrl) {
                    // Store the download URL in Firebase Database
                    dbRef.child("HangHoa").child(String.valueOf(hangHoa.getMaHangHoa()))
                            .child("hinhAnhUrl").setValue(downloadUrl)
                            .addOnSuccessListener(aVoid ->
                                    Log.d(TAG, "Image URL saved for HangHoa: " + hangHoa.getMaHangHoa()))
                            .addOnFailureListener(e ->
                                    Log.e(TAG, "Failed to save image URL for HangHoa: " + hangHoa.getMaHangHoa(), e));

                    // Cache the URL for local use
                    ImageCache.addUrlToCache(String.valueOf(hangHoa.getMaHangHoa()), downloadUrl);
                }

                @Override
                public void onFailure(Exception e) {
                    Log.e(TAG, "Failed to upload image for HangHoa: " + hangHoa.getMaHangHoa(), e);
                }
            });
        }
    }

    /**
     * Download image from Firebase Storage if needed
     */
    public void loadImageFromStorage(HangHoa hangHoa) {
        // If already has image data, nothing to do
        if (hangHoa.getHinhAnh() != null && hangHoa.getHinhAnh().length > 0) {
            return;
        }

        // Try to get URL from cache
        ImageCache.getUrlFromCache(String.valueOf(hangHoa.getMaHangHoa()), new OnUrlFetchedListener() {
            @Override
            public void onUrlFetched(String url) {
                if (url != null) {
                    downloadImageFromUrl(hangHoa, url);
                }
            }
        });

    }

    /**
     * Download image from URL and update local database
     */
    private void downloadImageFromUrl(HangHoa hangHoa, String imageUrl) {
        StorageUtils.downloadImage(imageUrl, new StorageUtils.OnDownloadCompleteListener() {
            @Override
            public void onSuccess(byte[] imageData) {
                // Update the model object
                hangHoa.setHinhAnh(imageData);

                // Update SQLite database
                SQLiteDatabase db = coffeeDB.getWritableDatabase();
                ContentValues values = new ContentValues();
                values.put("hinhAnh", imageData);
                db.update("HANGHOA", values, "maHangHoa=?",
                        new String[]{String.valueOf(hangHoa.getMaHangHoa())});

                // Update image cache
                ImageCache.addBitmapToMemoryCache(String.valueOf(hangHoa.getMaHangHoa()), imageData);

                Log.d(TAG, "Image downloaded for HangHoa: " + hangHoa.getMaHangHoa());
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Failed to download image for HangHoa: " + hangHoa.getMaHangHoa(), e);
            }
        });
    }

    /**
     * Interface để nhận callback khi lấy hàng hóa từ Firebase
     */
    public interface OnHangHoaListener {
        void onHangHoaReceived(HangHoa hangHoa);
        void onError(Exception e);
    }

    private ValueEventListener firebaseDirectListenerHangHoa;
    private OnHangHoaListener hangHoaListener;

    /**
     * Lấy hàng hóa trực tiếp từ Firebase theo mã (không sync về SQLite)
     * ✅ FIX CRITICAL: Capture listener trực tiếp, không dùng instance variable
     * 
     * @param maHangHoa Mã hàng hóa
     * @param listener Callback để nhận kết quả
     */
    public void getByMaHangHoaFromFirebaseDirect(int maHangHoa, OnHangHoaListener listener) {
        Log.d(TAG, "╔══════════════════════════════════════╗");
        Log.d(TAG, "║  getByMaHangHoaFromFirebaseDirect    ║");
        Log.d(TAG, "╚══════════════════════════════════════╝");
        Log.d(TAG, "➤ Tìm hàng hóa: maHangHoa=" + maHangHoa);
        
        // ✅ FIX CRITICAL: KHÔNG dùng instance variable
        // Capture listener trực tiếp trong closure
        
        // Tạo listener MỚI cho mỗi lần gọi (không reuse)
        ValueEventListener singleUseListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.d(TAG, "▶ Callback cho maHangHoa=" + maHangHoa);
                HangHoa foundHangHoa = null;
                
                // Xử lý dữ liệu từ Firebase
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    try {
                        if (snapshot.getValue() == null) {
                            continue;
                        }
                        
                        Map<String, Object> map = (Map<String, Object>) snapshot.getValue();
                        if (map != null) {
                            try {
                                HangHoa hangHoa = parseHangHoaFromMap(map);
                                
                                if (hangHoa != null && hangHoa.getMaHangHoa() == maHangHoa) {
                                    foundHangHoa = hangHoa;
                                    Log.d(TAG, "  ✓ Tìm thấy: " + hangHoa.getTenHangHoa() + 
                                          " (maHangHoa=" + hangHoa.getMaHangHoa() + ")");
                                    break;
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "  Lỗi parse hàng hóa: " + snapshot.getKey(), e);
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "  Lỗi xử lý snapshot: " + snapshot.getKey(), e);
                    }
                }
                
                // ✅ Callback với listener được capture trong closure (không dùng instance variable)
                final HangHoa finalHangHoa = foundHangHoa;
                runOnUiThread(() -> {
                    if (finalHangHoa != null) {
                        Log.d(TAG, "▶ Gọi listener.onHangHoaReceived cho: " + finalHangHoa.getTenHangHoa());
                        listener.onHangHoaReceived(finalHangHoa);
                    } else {
                        Log.e(TAG, "▶ Không tìm thấy hàng hóa với mã " + maHangHoa);
                        listener.onError(new Exception("Không tìm thấy hàng hóa với mã " + maHangHoa));
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Firebase listener cancelled cho maHangHoa=" + maHangHoa, error.toException());
                runOnUiThread(() -> {
                    listener.onError(error.toException());
                });
            }
        };
        
        // ✅ FIX: Sử dụng Firebase Query để filter hiệu quả
        Log.d(TAG, "▶ Attach listener với Query: orderByChild('maHangHoa').equalTo(" + maHangHoa + ")");
        dbRef.child(TABLE_NAME)
             .orderByChild("maHangHoa")  // ✅ Index theo maHangHoa
             .equalTo(maHangHoa)          // ✅ Chỉ lấy matching record
             .addListenerForSingleValueEvent(singleUseListener);
    }

    /**
     * Parse HangHoa từ Map
     */
    private HangHoa parseHangHoaFromMap(Map<String, Object> map) {
        try {
            HangHoa hangHoa = new HangHoa();
            
            // Parse maHangHoa
            Object maHangHoaObj = map.get("maHangHoa");
            if (maHangHoaObj instanceof Long) {
                hangHoa.setMaHangHoa(((Long) maHangHoaObj).intValue());
            } else if (maHangHoaObj instanceof Integer) {
                hangHoa.setMaHangHoa((Integer) maHangHoaObj);
            } else if (maHangHoaObj instanceof Double) {
                hangHoa.setMaHangHoa(((Double) maHangHoaObj).intValue());
            }
            
            // Parse maLoai
            Object maLoaiObj = map.get("maLoai");
            if (maLoaiObj instanceof Long) {
                hangHoa.setMaLoai(((Long) maLoaiObj).intValue());
            } else if (maLoaiObj instanceof Integer) {
                hangHoa.setMaLoai((Integer) maLoaiObj);
            } else if (maLoaiObj instanceof Double) {
                hangHoa.setMaLoai(((Double) maLoaiObj).intValue());
            }
            
            // Parse giaTien
            Object giaTienObj = map.get("giaTien");
            if (giaTienObj instanceof Long) {
                hangHoa.setGiaTien(((Long) giaTienObj).intValue());
            } else if (giaTienObj instanceof Integer) {
                hangHoa.setGiaTien((Integer) giaTienObj);
            } else if (giaTienObj instanceof Double) {
                hangHoa.setGiaTien(((Double) giaTienObj).intValue());
            }
            
            // Parse trangThai
            Object trangThaiObj = map.get("trangThai");
            if (trangThaiObj instanceof Long) {
                hangHoa.setTrangThai(((Long) trangThaiObj).intValue());
            } else if (trangThaiObj instanceof Integer) {
                hangHoa.setTrangThai((Integer) trangThaiObj);
            } else if (trangThaiObj instanceof Double) {
                hangHoa.setTrangThai(((Double) trangThaiObj).intValue());
            }
            
            // Parse tên hàng hóa
            hangHoa.setTenHangHoa((String) map.get("tenHangHoa"));
            
            // Firebase lưu hình ảnh dưới dạng URL (String), nhưng model local dùng byte[]
            // Tạm thời set null, sẽ load hình ảnh riêng từ Firebase Storage nếu cần
            hangHoa.setHinhAnh(null);
            
            return hangHoa;
        } catch (Exception e) {
            Log.e(TAG, "CRITICAL: Error parsing HangHoa from map", e);
            return null;
        }
    }

    /**
     * Dừng lắng nghe từ Firebase
     */
    public void stopFirebaseDirectListener() {
        if (firebaseDirectListenerHangHoa != null) {
            dbRef.child(TABLE_NAME).removeEventListener(firebaseDirectListenerHangHoa);
            firebaseDirectListenerHangHoa = null;
        }
        hangHoaListener = null;
    }

}