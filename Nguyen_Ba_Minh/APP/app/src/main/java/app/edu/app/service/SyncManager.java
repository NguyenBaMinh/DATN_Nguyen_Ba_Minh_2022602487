package app.edu.app.service;

import android.content.Context;
import android.util.Log;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

public class SyncManager {
    private static final String TAG = "SyncManager";
    private static final String SYNC_WORK_NAME = "coffee_sync_work";

    // Default sync interval (15 minutes)
    private static final long DEFAULT_SYNC_INTERVAL = 15;

    private Context context;
    private SyncPreferences syncPreferences;

    public SyncManager(Context context) {
        this.context = context;
        this.syncPreferences = new SyncPreferences(context);
    }

    /**
     * Initialize the sync system
     * Disabled immediate sync on app start to prevent continuous data loading
     */
    public void initialize() {
        Log.d(TAG, "Initializing sync manager");

        // Schedule periodic sync (every 15 minutes)
        schedulePeriodicSync();

        // Disabled immediate sync on app start to prevent continuous data loading
        // Sync will happen periodically or when explicitly requested
        // if (syncPreferences.isFirstSync() || syncPreferences.hasPendingChanges()) {
        //     Log.d(TAG, "Performing immediate sync due to first run or pending changes");
        //     performImmediateSync();
        // }
    }

    /**
     * Schedule a periodic sync operation
     */
    public void schedulePeriodicSync() {
        Log.d(TAG, "Scheduling periodic sync every " + DEFAULT_SYNC_INTERVAL + " minutes");

        // Define sync constraints (e.g., only when network is available)
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        // Create a periodic work request
        PeriodicWorkRequest syncRequest = new PeriodicWorkRequest.Builder(
                FirebaseSyncService.class,
                DEFAULT_SYNC_INTERVAL,
                TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build();

        // Enqueue the request in WorkManager
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                SYNC_WORK_NAME,
                ExistingPeriodicWorkPolicy.REPLACE,
                syncRequest);
    }

    /**
     * Perform an immediate sync operation
     */
    public void performImmediateSync() {
        Log.d(TAG, "Performing immediate sync");

        // Define sync constraints
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        // Create a one-time work request
        androidx.work.OneTimeWorkRequest syncRequest = new androidx.work.OneTimeWorkRequest.Builder(FirebaseSyncService.class)
                .setConstraints(constraints)
                .build();

        // Enqueue the request in WorkManager
        WorkManager.getInstance(context).enqueue(syncRequest);
    }

    /**
     * Note that changes have been made to local data that need to be synced
     */
    public void notifyLocalDataChanged(String tableName) {
        Log.d(TAG, "Local data changed in table: " + tableName);
        syncPreferences.setPendingChanges(true);
    }

    /**
     * Cancel all pending and future sync operations
     */
    public void cancelAllSync() {
        Log.d(TAG, "Cancelling all sync operations");
        WorkManager.getInstance(context).cancelUniqueWork(SYNC_WORK_NAME);
    }
}