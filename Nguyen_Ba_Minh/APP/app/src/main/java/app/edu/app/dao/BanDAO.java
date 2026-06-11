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
import app.edu.app.model.Ban;
import app.edu.app.utils.SyncUtils;

public class BanDAO extends FirebaseDAO {
    private static final String TABLE_NAME = "Ban";
    CoffeeDB coffeeDB;

    public BanDAO(Context context) {
        super(context);
        this.coffeeDB = new CoffeeDB(context);
    }

    @SuppressLint("Range")
    public ArrayList<Ban> get(String sql, String... choose) {
        ArrayList<Ban> list = new ArrayList<>();
        SQLiteDatabase sqLiteDatabase = coffeeDB.getReadableDatabase();
        @SuppressLint("Recycle") Cursor cursor = sqLiteDatabase.rawQuery(sql, choose);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            do {
                Ban ban = new Ban();
                ban.setMaBan(cursor.getInt(cursor.getColumnIndex("maBan")));
                ban.setTrangThai(cursor.getInt(cursor.getColumnIndex("trangThai")));
                list.add(ban);
                // Removed verbose logging to improve performance
            } while (cursor.moveToNext());
        }

        return list;
    }

    // Add this at class level
    private RealtimeUpdateCallback banCallback;

    /**
     * Start real-time synchronization for Ban table
     * Optimized to process updates in background thread
     */
    public void startRealtimeSync() {
        banCallback = new RealtimeUpdateCallback() {
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
                    ArrayList<Ban> toInsert = new ArrayList<>();
                    ArrayList<Ban> toUpdate = new ArrayList<>();
                    
                    for (Map<String, Object> map : dataList) {
                        try {
                            Ban firebaseBan = new Ban();
                            firebaseBan.setMaBan(((Long) map.get("maBan")).intValue());
                            firebaseBan.setTrangThai(((Long) map.get("trangThai")).intValue());
                            
                            try {
                                Ban localBan = getByMaBan(String.valueOf(firebaseBan.getMaBan()));
                                // Check if update is needed
                                if (localBan.getTrangThai() != firebaseBan.getTrangThai()) {
                                    toUpdate.add(firebaseBan);
                                }
                            } catch (Exception e) {
                                // Item doesn't exist locally, insert it
                                toInsert.add(firebaseBan);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing Firebase Ban update", e);
                        }
                    }
                    
                    // Batch insert
                    for (Ban ban : toInsert) {
                        insertBanFromFirebase(ban);
                    }
                    
                    // Batch update
                    for (Ban ban : toUpdate) {
                        updateBanFromFirebase(ban);
                    }
                    
                    if (!toInsert.isEmpty() || !toUpdate.isEmpty()) {
                        Log.d(TAG, "Realtime sync: " + toInsert.size() + " inserted, " + toUpdate.size() + " updated");
                    }
                });
            }
        };

        setupRealtimeListener(TABLE_NAME, banCallback);
    }

    /**
     * Stop real-time synchronization
     */
    public void stopRealtimeSync() {
        removeRealtimeListener(TABLE_NAME);
    }

    // Add these methods to avoid triggering a sync loop
    private boolean updateBanFromFirebase(Ban ban) {
        SQLiteDatabase sqLiteDatabase = coffeeDB.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("trangThai", ban.getTrangThai());
        int check = sqLiteDatabase.update("BAN", values, "maBan=?", new String[]{String.valueOf(ban.getMaBan())});

        if (check > 0) {
            Log.d(TAG, "Updated Ban from Firebase: " + ban.getMaBan());
        }

        return check > 0;
    }

    private boolean insertBanFromFirebase(Ban ban) {
        SQLiteDatabase sqLiteDatabase = coffeeDB.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("maBan", ban.getMaBan());
        values.put("trangThai", ban.getTrangThai());
        long check = sqLiteDatabase.insert("BAN", null, values);

        if (check != -1) {
            Log.d(TAG, "Inserted Ban from Firebase: " + ban.getMaBan());
        }

        return check != -1;
    }

    // Modify existing methods to set isLocalUpdate flag


    /**
     * Get all tables from local database only (non-blocking)
     * Use this for UI display to avoid blocking the main thread
     */
    public ArrayList<Ban> getAllLocal() {
        String sqlGetAll = "SELECT * FROM BAN ORDER BY maBan";
        return get(sqlGetAll);
    }

    /**
     * Get all tables with automatic sync from Firebase
     * Note: This will return local data immediately and sync in background
     */
    public ArrayList<Ban> getAll() {
        // Return local data immediately
        ArrayList<Ban> list = getAllLocal();

        // Fetch updates from Firebase in background (non-blocking)
        if (isNetworkAvailable() && shouldSync(TABLE_NAME)) {
            fetchFromFirebaseAsync();
        }

        return list;
    }

    // Fetch data from Firebase asynchronously in background thread
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
                
                // Now process all data in background thread
                runInBackground(() -> {
                    ArrayList<Ban> toInsert = new ArrayList<>();
                    ArrayList<Ban> toUpdate = new ArrayList<>();
                    
                    for (Map<String, Object> map : dataList) {
                        try {
                            Ban firebaseBan = new Ban();
                            firebaseBan.setMaBan(((Long) map.get("maBan")).intValue());
                            firebaseBan.setTrangThai(((Long) map.get("trangThai")).intValue());
                            
                            // Check if item exists locally (on background thread)
                            try {
                                Ban localBan = getByMaBan(String.valueOf(firebaseBan.getMaBan()));
                                
                                // If local version is different, mark for update
                                if (localBan.getTrangThai() != firebaseBan.getTrangThai()) {
                                    toUpdate.add(firebaseBan);
                                }
                            } catch (Exception e) {
                                // Item doesn't exist locally, mark for insert
                                toInsert.add(firebaseBan);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing Firebase data", e);
                        }
                    }
                    
                    // Batch insert
                    for (Ban ban : toInsert) {
                        insertBanFromFirebase(ban);
                    }
                    
                    // Batch update
                    for (Ban ban : toUpdate) {
                        updateBanFromFirebase(ban);
                    }
                    
                    Log.d(TAG, "Sync completed: " + toInsert.size() + " inserted, " + toUpdate.size() + " updated");
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Firebase fetch cancelled", error.toException());
            }
        });
    }


    public boolean insertBan(Ban ban) {
        SQLiteDatabase sqLiteDatabase = coffeeDB.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("trangThai", ban.getTrangThai());
        long check = sqLiteDatabase.insert("BAN", null, values);

        // If local insert successful and network available, sync to Firebase
        if (check != -1 && isNetworkAvailable()) {
            isLocalUpdate = false;
            syncToFirebase(ban);
        }
        isLocalUpdate = true;

        return check != -1;
    }

    public boolean deleteBan(String maBan) {
        SQLiteDatabase sqLiteDatabase = coffeeDB.getWritableDatabase();
        int check = sqLiteDatabase.delete("BAN", "maBan=?", new String[]{maBan});

        // If local delete successful and network available, delete from Firebase
        if (check > 0 && isNetworkAvailable()) {
            isLocalUpdate = false;
            deleteFromFirebase(maBan);
        }
        isLocalUpdate = true;

        return check > 0;
    }

    public boolean updateBan(Ban ban) {
        SQLiteDatabase sqLiteDatabase = coffeeDB.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("trangThai", ban.getTrangThai());
        int check = sqLiteDatabase.update("BAN", values, "maBan=?", new String[]{String.valueOf(ban.getMaBan())});

        // If local update successful and network available, sync to Firebase
        if (check > 0 && isNetworkAvailable()) {
            isLocalUpdate = false;
            syncToFirebase(ban);
        }
        isLocalUpdate = true;

        return check > 0;
    }

    public Ban getByMaBan(String maBan) {
        String sqlGetByMaLoai = "SELECT * FROM BAN WHERE maBan=?";
        ArrayList<Ban> list = get(sqlGetByMaLoai, maBan);

        return list.get(0);
    }

    // Sync a Ban object to Firebase
    private void syncToFirebase(Ban ban) {
        Map<String, Object> banMap = SyncUtils.convertBanToMap(ban);

        dbRef.child(TABLE_NAME).child(String.valueOf(ban.getMaBan()))
                .setValue(banMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        logSuccessfulSync("Sync", String.valueOf(ban.getMaBan()));
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        handleSyncError("Sync", String.valueOf(ban.getMaBan()), e);
                    }
                });
    }

    // Delete a Ban from Firebase
    private void deleteFromFirebase(String maBan) {
        dbRef.child(TABLE_NAME).child(maBan)
                .removeValue()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        logSuccessfulSync("Delete", maBan);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        handleSyncError("Delete", maBan, e);
                    }
                });
    }

    /**
     * Interface để nhận danh sách Ban từ Firebase
     */
    public interface OnBanListListener {
        void onBanListReceived(ArrayList<Ban> banList);
        void onError(Exception e);
    }

    /**
     * Real-time listener để lắng nghe thay đổi từ Firebase
     */
    private ValueEventListener firebaseRealTimeListener;
    private OnBanListListener banListListener;

    /**
     * Lấy danh sách bàn trực tiếp từ Firebase Realtime Database
     * Tự động cập nhật real-time khi có thay đổi
     * @param listener Callback để nhận danh sách bàn
     */
    public void getAllFromFirebaseDirect(OnBanListListener listener) {
        this.banListListener = listener;
        
        // Remove listener cũ nếu có
        if (firebaseRealTimeListener != null) {
            dbRef.child(TABLE_NAME).removeEventListener(firebaseRealTimeListener);
        }
        
        // Tạo real-time listener
        firebaseRealTimeListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                ArrayList<Ban> banList = new ArrayList<>();
                
                // Xử lý dữ liệu từ Firebase (có thể là array hoặc object)
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    try {
                        // Bỏ qua giá trị null
                        if (snapshot.getValue() == null) {
                            continue;
                        }
                        
                        Map<String, Object> map = (Map<String, Object>) snapshot.getValue();
                        if (map != null) {
                            Ban ban = new Ban();
                            Object maBanObj = map.get("maBan");
                            Object trangThaiObj = map.get("trangThai");
                            
                            if (maBanObj != null && trangThaiObj != null) {
                                // Xử lý maBan có thể là Long hoặc Integer
                                if (maBanObj instanceof Long) {
                                    ban.setMaBan(((Long) maBanObj).intValue());
                                } else if (maBanObj instanceof Integer) {
                                    ban.setMaBan((Integer) maBanObj);
                                } else if (maBanObj instanceof Double) {
                                    ban.setMaBan(((Double) maBanObj).intValue());
                                }
                                
                                // Xử lý trangThai có thể là Long hoặc Integer
                                if (trangThaiObj instanceof Long) {
                                    ban.setTrangThai(((Long) trangThaiObj).intValue());
                                } else if (trangThaiObj instanceof Integer) {
                                    ban.setTrangThai((Integer) trangThaiObj);
                                } else if (trangThaiObj instanceof Double) {
                                    ban.setTrangThai(((Double) trangThaiObj).intValue());
                                }
                                
                                banList.add(ban);
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing Ban from Firebase: " + snapshot.getKey(), e);
                    }
                }
                
                // Sort by maBan
                banList.sort((b1, b2) -> Integer.compare(b1.getMaBan(), b2.getMaBan()));
                
                // Callback về main thread
                runOnUiThread(() -> {
                    if (banListListener != null) {
                        banListListener.onBanListReceived(banList);
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Firebase listener cancelled", error.toException());
                runOnUiThread(() -> {
                    if (banListListener != null) {
                        banListListener.onError(error.toException());
                    }
                });
            }
        };
        
        // Attach listener
        dbRef.child(TABLE_NAME).addValueEventListener(firebaseRealTimeListener);
    }

    /**
     * Dừng lắng nghe real-time từ Firebase
     */
    public void stopFirebaseDirectListener() {
        if (firebaseRealTimeListener != null) {
            dbRef.child(TABLE_NAME).removeEventListener(firebaseRealTimeListener);
            firebaseRealTimeListener = null;
        }
        banListListener = null;
    }
    
    // ===================== FIREBASE DIRECT MODE - SINGLE OBJECT =====================
    
    /**
     * Interface callback cho Firebase Direct mode (single object)
     */
    public interface OnBanListener {
        void onBanReceived(Ban ban);
        void onError(Exception e);
    }
    
    /**
     * Lấy bàn TRỰC TIẾP từ Firebase (không qua SQLite) theo mã bàn
     * 
     * @param maBan Mã bàn cần lấy
     * @param listener Callback nhận kết quả
     */
    public void getByMaBanFromFirebaseDirect(String maBan, OnBanListener listener) {
        dbRef.child(TABLE_NAME).child(maBan)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        try {
                            if (snapshot.exists()) {
                                Map<String, Object> map = (Map<String, Object>) snapshot.getValue();
                                if (map != null) {
                                    Ban ban = new Ban();
                                    
                                    // Parse maBan
                                    Object maBanObj = map.get("maBan");
                                    if (maBanObj instanceof Long) {
                                        ban.setMaBan(((Long) maBanObj).intValue());
                                    } else if (maBanObj instanceof Integer) {
                                        ban.setMaBan((Integer) maBanObj);
                                    } else if (maBanObj instanceof Double) {
                                        ban.setMaBan(((Double) maBanObj).intValue());
                                    }
                                    
                                    // Parse trangThai
                                    Object trangThaiObj = map.get("trangThai");
                                    if (trangThaiObj instanceof Long) {
                                        ban.setTrangThai(((Long) trangThaiObj).intValue());
                                    } else if (trangThaiObj instanceof Integer) {
                                        ban.setTrangThai((Integer) trangThaiObj);
                                    } else if (trangThaiObj instanceof Double) {
                                        ban.setTrangThai(((Double) trangThaiObj).intValue());
                                    }
                                    
                                    Log.d(TAG, "✓ Load bàn từ Firebase: " + ban.getMaBan() + 
                                          ", trạng thái=" + ban.getTrangThai());
                                    listener.onBanReceived(ban);
                                } else {
                                    listener.onError(new Exception("Dữ liệu bàn null"));
                                }
                            } else {
                                listener.onError(new Exception("Không tìm thấy bàn: " + maBan));
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Lỗi parse bàn từ Firebase", e);
                            listener.onError(e);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Firebase bị hủy khi load bàn", error.toException());
                        listener.onError(error.toException());
                    }
                });
    }
}