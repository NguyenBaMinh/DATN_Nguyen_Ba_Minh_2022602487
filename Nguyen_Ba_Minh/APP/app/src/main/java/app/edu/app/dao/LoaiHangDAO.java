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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import app.edu.app.database.CoffeeDB;
import app.edu.app.interfaces.OnUrlFetchedListener;
import app.edu.app.interfaces.RealtimeUpdateCallback;
import app.edu.app.model.LoaiHang;
import app.edu.app.utils.ImageCache;
import app.edu.app.utils.StorageUtils;
import app.edu.app.utils.SyncUtils;

public class LoaiHangDAO extends FirebaseDAO {
    private static final String TABLE_NAME = "LoaiHang";
    CoffeeDB coffeeDB;

    public LoaiHangDAO(Context context) {
        super(context);
        this.coffeeDB = new CoffeeDB(context);
    }

    @SuppressLint("Range")
    public ArrayList<LoaiHang> get(String sql, String... choose) {
        ArrayList<LoaiHang> list = new ArrayList<>();
        SQLiteDatabase sqLiteDatabase = coffeeDB.getReadableDatabase();
        @SuppressLint("Recycle") Cursor cursor = sqLiteDatabase.rawQuery(sql, choose);
        if (cursor.getCount() != 0) {
            cursor.moveToFirst();
            do {
                LoaiHang loaiHang = new LoaiHang();
                loaiHang.setMaLoai(cursor.getInt(cursor.getColumnIndex("maLoai")));
                loaiHang.setTenLoai(cursor.getString(cursor.getColumnIndex("tenLoai")));
                loaiHang.setHinhAnh(cursor.getBlob(cursor.getColumnIndex("hinhAnh")));
                list.add(loaiHang);
                Log.i("TAG", loaiHang.toString());
            } while (cursor.moveToNext());
        }

        return list;
    }

    public ArrayList<LoaiHang> getAll() {
        String sqlGetAll = "SELECT * FROM LOAIHANG";
        ArrayList<LoaiHang> list = get(sqlGetAll);

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
                            LoaiHang firebaseLoaiHang = SyncUtils.convertMapToLoaiHang(map);

                            // Check if item exists locally
                            try {
                                LoaiHang localLoaiHang = getByMaLoai(String.valueOf(firebaseLoaiHang.getMaLoai()));

                                // If local version is different, update it
                                if (!localLoaiHang.getTenLoai().equals(firebaseLoaiHang.getTenLoai())) {
                                    updateLoaiHang(firebaseLoaiHang);
                                }
                            } catch (Exception e) {
                                // Item doesn't exist locally, insert it
                                insertLoaiHang(firebaseLoaiHang);
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

    // Add this at class level
    private RealtimeUpdateCallback loaiHangCallback;

    /**
     * Start real-time synchronization for LoaiHang table
     */
    public void startRealtimeSync() {
        loaiHangCallback = new RealtimeUpdateCallback() {
            @Override
            public void onDataChanged(DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    try {
                        Map<String, Object> map = (Map<String, Object>) snapshot.getValue();
                        if (map != null) {
                            LoaiHang firebaseLoaiHang = SyncUtils.convertMapToLoaiHang(map);

                            try {
                                LoaiHang localLoaiHang = getByMaLoai(String.valueOf(firebaseLoaiHang.getMaLoai()));
                                // Update if there are differences
                                updateLoaiHangFromFirebase(firebaseLoaiHang);
                            } catch (Exception e) {
                                // Item doesn't exist locally, insert it
                                insertLoaiHangFromFirebase(firebaseLoaiHang);
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing Firebase LoaiHang update", e);
                    }
                }
            }
        };

        setupRealtimeListener(TABLE_NAME, loaiHangCallback);
    }

    /**
     * Stop real-time synchronization
     */
    public void stopRealtimeSync() {
        removeRealtimeListener(TABLE_NAME);
    }

    // Methods to handle Firebase updates without triggering sync loop
    private boolean updateLoaiHangFromFirebase(LoaiHang loaiHang) {
        SQLiteDatabase sqLiteDatabase = coffeeDB.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("tenLoai", loaiHang.getTenLoai());
        values.put("hinhAnh", loaiHang.getHinhAnh());

        int check = sqLiteDatabase.update("LOAIHANG", values, "maLoai=?",
                new String[]{String.valueOf(loaiHang.getMaLoai())});

        if (check > 0) {
            Log.d(TAG, "Updated LoaiHang from Firebase: " + loaiHang.getMaLoai());
        }

        return check > 0;
    }

    private boolean insertLoaiHangFromFirebase(LoaiHang loaiHang) {
        SQLiteDatabase sqLiteDatabase = coffeeDB.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("maLoai", loaiHang.getMaLoai());
        values.put("tenLoai", loaiHang.getTenLoai());
        values.put("hinhAnh", loaiHang.getHinhAnh());

        long check = sqLiteDatabase.insert("LOAIHANG", null, values);

        if (check != -1) {
            isLocalUpdate = true;
            Log.d(TAG, "Inserted LoaiHang from Firebase: " + loaiHang.getMaLoai());
        }
        isLocalUpdate = false;

        return check != -1;
    }

    // Modify existing CRUD methods

    public boolean insertLoaiHang(LoaiHang loaiHang) {
        SQLiteDatabase sqLiteDatabase = coffeeDB.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("tenLoai", loaiHang.getTenLoai());
        values.put("hinhAnh", loaiHang.getHinhAnh());
        long check = sqLiteDatabase.insert("LOAIHANG", null, values);

        // If local insert successful and network available, sync to Firebase
        if (check != -1 && isNetworkAvailable()) {
            // Get the auto-generated ID and set it to the object
            if (loaiHang.getMaLoai() == 0) {
                String query = "SELECT last_insert_rowid() as lastId";
                Cursor cursor = sqLiteDatabase.rawQuery(query, null);
                if (cursor.moveToFirst()) {
                    @SuppressLint("Range") int lastId = cursor.getInt(cursor.getColumnIndex("lastId"));
                    loaiHang.setMaLoai(lastId);
                }
                cursor.close();
            }
            isLocalUpdate = true;

            syncToFirebase(loaiHang);
        }
        isLocalUpdate = false;

        return check != -1;
    }

    public boolean updateLoaiHang(LoaiHang loaiHang) {
        SQLiteDatabase sqLiteDatabase = coffeeDB.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("tenLoai", loaiHang.getTenLoai());
        values.put("hinhAnh", loaiHang.getHinhAnh());
        int check = sqLiteDatabase.update("LOAIHANG", values, "maLoai=?", new String[]{String.valueOf(loaiHang.getMaLoai())});

        // If local update successful and network available, sync to Firebase
        if (check > 0 && isNetworkAvailable()) {
            isLocalUpdate = true;
            syncToFirebase(loaiHang);
        }
        isLocalUpdate = false;

        return check > 0;
    }

    public boolean deleteLoaiHang(String maLoai) {
        SQLiteDatabase sqLiteDatabase = coffeeDB.getWritableDatabase();

        @SuppressLint("Recycle") Cursor cursor = sqLiteDatabase.rawQuery("SELECT * FROM HANGHOA WHERE maLoai=?", new String[]{maLoai});
        if (cursor.getCount() != 0) {
            return false;
        }
        int check = sqLiteDatabase.delete("LOAIHANG", "maLoai=?", new String[]{maLoai});

        // If local delete successful and network available, delete from Firebase
        if (check > 0 && isNetworkAvailable()) {
            deleteFromFirebase(maLoai);
            isLocalUpdate = true;
        }
        isLocalUpdate = false;

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


    public LoaiHang getByMaLoai(String maLoai) {
        String sqlGetByMaLoai = "SELECT * FROM LOAIHANG WHERE maLoai=?";
        ArrayList<LoaiHang> list = get(sqlGetByMaLoai, maLoai);

        return list.get(0);
    }

    public LoaiHang getbyTenLoai(String tenLoai) {
        String sqlGetByMaLoai = "SELECT * FROM LOAIHANG WHERE tenLoai=?";
        ArrayList<LoaiHang> list = get(sqlGetByMaLoai, tenLoai);

        return list.get(0);
    }

    // Sync a LoaiHang object to Firebase
    private void syncToFirebase(LoaiHang loaiHang) {
        Map<String, Object> loaiHangMap = SyncUtils.convertLoaiHangToMap(loaiHang);

        dbRef.child(TABLE_NAME).child(String.valueOf(loaiHang.getMaLoai()))
                .setValue(loaiHangMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        logSuccessfulSync("Sync", String.valueOf(loaiHang.getMaLoai()));
                        // Upload image separately after data is saved
                        if (loaiHang.getHinhAnh() != null && loaiHang.getHinhAnh().length > 0) {
                            uploadImageToStorage(loaiHang);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        handleSyncError("Sync", String.valueOf(loaiHang.getMaLoai()), e);
                    }
                });
    }

    /**
     * Upload image to Firebase Storage
     */
    private void uploadImageToStorage(LoaiHang loaiHang) {
        if (loaiHang.getHinhAnh() != null && loaiHang.getHinhAnh().length > 0) {
            String path = "loaihang/" + loaiHang.getMaLoai() + ".jpg";

            StorageUtils.uploadImage(loaiHang.getHinhAnh(), path, new StorageUtils.OnUploadCompleteListener() {
                @Override
                public void onSuccess(String downloadUrl) {
                    // Store the download URL in Firebase Database
                    dbRef.child("LoaiHang").child(String.valueOf(loaiHang.getMaLoai()))
                            .child("hinhAnhUrl").setValue(downloadUrl)
                            .addOnSuccessListener(aVoid ->
                                    Log.d(TAG, "Image URL saved for LoaiHang: " + loaiHang.getMaLoai()))
                            .addOnFailureListener(e ->
                                    Log.e(TAG, "Failed to save image URL for LoaiHang: " + loaiHang.getMaLoai(), e));

                    // Cache the URL for local use
                    ImageCache.addUrlToCache("loaihang_" + loaiHang.getMaLoai(), downloadUrl);
                }

                @Override
                public void onFailure(Exception e) {
                    Log.e(TAG, "Failed to upload image for LoaiHang: " + loaiHang.getMaLoai(), e);
                }
            });
        }
    }

    /**
     * Download image from Firebase Storage if needed
     */
    public void loadImageFromStorage(LoaiHang loaiHang) {
        // If already has image data, nothing to do
        if (loaiHang.getHinhAnh() != null && loaiHang.getHinhAnh().length > 0) {
            return;
        }

        // Try to get URL from cache
        ImageCache.getUrlFromCache("loaihang_" + loaiHang.getMaLoai(), new OnUrlFetchedListener() {
            @Override
            public void onUrlFetched(String url) {
                if (url != null) {
                    downloadImageFromUrl(loaiHang, url);
                }
            }
        });

        // If not in cache, try to get from Firebase

    }

    /**
     * Download image from URL and update local database
     */
    private void downloadImageFromUrl(LoaiHang loaiHang, String imageUrl) {
        StorageUtils.downloadImage(imageUrl, new StorageUtils.OnDownloadCompleteListener() {
            @Override
            public void onSuccess(byte[] imageData) {
                // Update the model object
                loaiHang.setHinhAnh(imageData);

                // Update SQLite database
                SQLiteDatabase db = coffeeDB.getWritableDatabase();
                ContentValues values = new ContentValues();
                values.put("hinhAnh", imageData);
                db.update("LOAIHANG", values, "maLoai=?",
                        new String[]{String.valueOf(loaiHang.getMaLoai())});

                Log.d(TAG, "Image downloaded for LoaiHang: " + loaiHang.getMaLoai());
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Failed to download image for LoaiHang: " + loaiHang.getMaLoai(), e);
            }
        });
    }

    // Delete a LoaiHang from Firebase
    private void deleteFromFirebase(String maLoai) {
        dbRef.child(TABLE_NAME).child(maLoai)
                .removeValue()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        logSuccessfulSync("Delete", maLoai);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        handleSyncError("Delete", maLoai, e);
                    }
                });
    }
}