package app.edu.app;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.FirebaseDatabase;

import app.edu.app.dao.BanDAO;
import app.edu.app.dao.HangHoaDAO;
import app.edu.app.dao.HoaDonChiTietDAO;
import app.edu.app.dao.HoaDonDAO;
import app.edu.app.dao.HoaDonMangVeDao;
import app.edu.app.dao.LoaiHangDAO;
import app.edu.app.dao.NguoiDungDAO;
import app.edu.app.dao.ThongBaoDAO;
import app.edu.app.service.NetworkMonitor;
import app.edu.app.service.SyncManager;
import app.edu.app.utils.ImageCache;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Application class to initialize Firebase and sync components
 * Optimized to prevent UI freezing during startup
 */
public class APTCoffeeApplication extends Application {
    private static final String TAG = "APTCoffeeApplication";

    // Singleton instance
    private static APTCoffeeApplication instance;

    // Background thread pool
    private final ExecutorService executorService = Executors.newFixedThreadPool(5);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private SyncManager syncManager;
    private NetworkMonitor networkMonitor;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Application onCreate started");

        instance = this;

        // Initialize Firebase
        Log.d(TAG, "About to initialize Firebase");
        FirebaseApp.initializeApp(this);

        Log.d(TAG, "About to enable Firebase persistence");
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        Log.d(TAG, "Firebase persistence enabled");
        // Rest of your initialization code...
        ImageCache.initialize(this);
        executorService.execute(this::initializeSyncComponents);
    }

    /**
     * Initialize synchronization components in background thread
     */

    // Add this method to your APTCoffeeApplication class
    private void initializeRealtimeSyncing() {
        Log.d(TAG, "Initializing real-time data synchronization");

        // Initialize DAOs
        BanDAO banDAO = new BanDAO(this);
        HangHoaDAO hangHoaDAO = new HangHoaDAO(this);
        LoaiHangDAO loaiHangDAO = new LoaiHangDAO(this);
        NguoiDungDAO nguoiDungDAO = new NguoiDungDAO(this);
        HoaDonDAO hoaDonDAO = new HoaDonDAO(this);
        HoaDonChiTietDAO hoaDonChiTietDAO = new HoaDonChiTietDAO(this);
        HoaDonMangVeDao hoaDonMangVeDao = new HoaDonMangVeDao(this);
        ThongBaoDAO thongBaoDAO = new ThongBaoDAO(this);

        // Start real-time listeners for each table
        banDAO.startRealtimeSync();
        hangHoaDAO.startRealtimeSync();
        loaiHangDAO.startRealtimeSync();
        nguoiDungDAO.startRealtimeSync();
        hoaDonDAO.startRealtimeSync();
        hoaDonChiTietDAO.startRealtimeSync();
        hoaDonMangVeDao.startRealtimeSync();
        thongBaoDAO.startRealtimeSync();
    }

    // And call this method in the initializeSyncComponents method
    private void initializeSyncComponents() {
        Log.d(TAG, "Initializing synchronization system in background");

        try {
            // Create network monitor first to detect connectivity
            networkMonitor = new NetworkMonitor(this);

            // Start network monitoring
            networkMonitor.startMonitoring();

            // Initialize sync manager after network is ready
            syncManager = new SyncManager(this);

            // Disable auto sync initialization on app start to prevent continuous data loading
            // Sync will happen manually when needed or periodically (every 15 minutes)
            // if (networkMonitor.isNetworkAvailable()) {
            //     syncManager.initialize();
            //     initializeRealtimeSyncing();
            // }

            // Log completion on main thread
            mainHandler.post(() -> {
                Log.d(TAG, "Synchronization system initialized successfully");
            });
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize sync components", e);
        }
    }


    /**
     * Get the application instance
     */
    public static APTCoffeeApplication getInstance() {
        return instance;
    }

    /**
     * Get the sync manager
     */
    public SyncManager getSyncManager() {
        return syncManager;
    }

    /**
     * Get the network monitor
     */
    public NetworkMonitor getNetworkMonitor() {
        return networkMonitor;
    }

    /**
     * Execute a task on a background thread
     */
    public void executeInBackground(Runnable task) {
        executorService.execute(task);
    }

    /**
     * Execute a task on the main thread
     */
    public void executeOnMainThread(Runnable task) {
        mainHandler.post(task);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();

        // Stop network monitoring
        if (networkMonitor != null) {
            networkMonitor.stopMonitoring();
        }

        // Cancel all sync operations
        if (syncManager != null) {
            syncManager.cancelAllSync();
        }

        // Shutdown executor service
        executorService.shutdown();
    }

    /**
     * Force an immediate sync if network is available
     */
    public void forceSync() {
        if (syncManager != null && networkMonitor != null && networkMonitor.isNetworkAvailable()) {
            syncManager.performImmediateSync();
        }
    }
}