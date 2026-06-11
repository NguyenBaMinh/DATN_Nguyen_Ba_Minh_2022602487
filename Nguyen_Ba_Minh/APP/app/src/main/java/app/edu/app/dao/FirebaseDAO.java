package app.edu.app.dao;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseException;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import app.edu.app.interfaces.RealtimeUpdateCallback;
import app.edu.app.service.SyncPreferences;

/**
 * Base class for Firebase-synchronized DAOs
 * Optimized to prevent UI freezing
 */
public abstract class FirebaseDAO {
    protected static final String TAG = "FirebaseDAO";

    // Constants for sync timing
    protected static final long SYNC_INTERVAL = 5 * 1000; // 5 minutes

    // Thread pool for background operations
    protected static final ExecutorService executor = Executors.newFixedThreadPool(5);
    protected static final Handler mainHandler = new Handler(Looper.getMainLooper());

    protected Context context;
    protected FirebaseDatabase database;
    protected DatabaseReference dbRef;
    protected SyncPreferences syncPreferences;

    public FirebaseDAO(Context context) {
        this.context = context;

        // Initialize Firebase
        database = FirebaseDatabase.getInstance();
// In FirebaseDAO.java constructor
        try {
            FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        } catch (DatabaseException e) {
            // Persistence is already enabled, which is fine
            Log.d("FirebaseDAO", "Persistence already enabled");
        }
        dbRef = database.getReference();

        // Initialize sync preferences
        syncPreferences = new SyncPreferences(context);
    }

    /**
     * Run a task on a background thread
     */
    protected void runInBackground(Runnable task) {
        executor.execute(task);
    }

    /**
     * Run a task on the main thread
     */
    protected void runOnUiThread(Runnable task) {
        mainHandler.post(task);
    }

    /**
     * Check if network is available
     */
    protected boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }

        return false;
    }

    /**
     * Handle a sync error
     */
    protected void handleSyncError(String operation, String id, Exception e) {
        Log.e(TAG, operation + " failed for ID: " + id, e);
    }

    /**
     * Log a successful sync
     */
    protected void logSuccessfulSync(String operation, String id) {
        Log.d(TAG, operation + " successful for ID: " + id);
    }

    /**
     * Set up a listener for real-time updates from Firebase
     * @param tableName The Firebase node name to listen to
     * @param callback Callback to execute when data changes
     */
    // Add these fields to FirebaseDAO class
    protected ValueEventListener realtimeListener;
    protected boolean isLocalUpdate = false;

    /**
     * Set up a listener for real-time updates from Firebase
     * @param tableName The Firebase node name to listen to
     * @param callback Callback to execute when data changes
     */
    protected void setupRealtimeListener(final String tableName, final RealtimeUpdateCallback callback) {
        if (dbRef == null) return;

        // Remove existing listener if there is one
        if (realtimeListener != null) {
            dbRef.child(tableName).removeEventListener(realtimeListener);
        }

        realtimeListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (isLocalUpdate) {
                    // Skip processing if this update was triggered locally
                    Log.d(TAG, "Skipping local update for " + tableName);
                    return;
                }

                Log.d(TAG, "Realtime update received for " + tableName);
                callback.onDataChanged(dataSnapshot);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Realtime listener cancelled for " + tableName, error.toException());
            }
        };

        dbRef.child(tableName).addValueEventListener(realtimeListener);
        Log.d(TAG, "Realtime listener set up for " + tableName);
    }

    /**
     * Remove the realtime listener
     * @param tableName The Firebase node name to stop listening to
     */
    protected void removeRealtimeListener(String tableName) {
        if (dbRef != null && realtimeListener != null) {
            dbRef.child(tableName).removeEventListener(realtimeListener);
            realtimeListener = null;
            Log.d(TAG, "Realtime listener removed for " + tableName);
        }
    }

    /**
     * Clean up resources when DAO is no longer needed
     */
    public void cleanup() {
        if (realtimeListener != null) {
            Log.d(TAG, "Cleaning up realtime listeners");
            // Implementation would depend on how you track which tables have listeners
        }
    }

    /**
     * Check if sync is needed based on last sync time
     */
    protected boolean shouldSync(String tableName) {
        long lastSyncTime = 0;
        switch (tableName) {
            case "Ban":
                lastSyncTime = syncPreferences.getBanLastSyncTime();
                break;
            case "HangHoa":
                lastSyncTime = syncPreferences.getHangHoaLastSyncTime();
                break;
            case "HoaDon":
                lastSyncTime = syncPreferences.getHoaDonLastSyncTime();
                break;
            case "HoaDonChiTiet":
                lastSyncTime = syncPreferences.getHoaDonChiTietLastSyncTime();
                break;
            case "HoaDonMangVe":
                lastSyncTime = syncPreferences.getHoaDonMangVeLastSyncTime();
                break;
            case "LoaiHang":
                lastSyncTime = syncPreferences.getLoaiHangLastSyncTime();
                break;
            case "NguoiDung":
                lastSyncTime = syncPreferences.getNguoiDungLastSyncTime();
                break;
            case "ThongBao":
                lastSyncTime = syncPreferences.getThongBaoLastSyncTime();
                break;
        }

        long currentTime = System.currentTimeMillis();
        return (currentTime - lastSyncTime) > SYNC_INTERVAL;
    }

    /**
     * Interface for operation callbacks
     */
    public interface OperationCallback {
        void onSuccess();

        void onFailure(Exception e);
    }
}