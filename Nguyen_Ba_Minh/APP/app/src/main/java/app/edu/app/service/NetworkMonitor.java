package app.edu.app.service;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.util.Log;
public class NetworkMonitor {
    private static final String TAG = "NetworkMonitor";

    private Context context;
    private SyncManager syncManager;
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;

    public NetworkMonitor(Context context) {
        this.context = context;
        this.syncManager = new SyncManager(context);
        this.connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    /**
     * Bắt đầu theo dõi thay đổi mạng
     */
    public void startMonitoring() {
        Log.d(TAG, "Bắt đầu theo dõi mạng");

        // Tạo callback cho mạng
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                super.onAvailable(network);
                Log.d(TAG, "Kết nối mạng khả dụng");
                // Disabled auto sync to prevent continuous data loading
                // Sync will happen manually when needed or periodically
                // syncManager.performImmediateSync();
            }

            @Override
            public void onLost(Network network) {
                super.onLost(network);
                Log.d(TAG, "Mất kết nối mạng");
            }
        };

        // Đăng ký network callback
        NetworkRequest networkRequest = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();

        connectivityManager.registerNetworkCallback(networkRequest, networkCallback);
    }

    /**
     * Dừng theo dõi thay đổi mạng
     */
    public void stopMonitoring() {
        Log.d(TAG, "Dừng theo dõi mạng");

        if (networkCallback != null) {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback);
                networkCallback = null;
            } catch (Exception e) {
                Log.e(TAG, "Lỗi khi hủy đăng ký network callback", e);
            }
        }
    }

    /**
     * Kiểm tra mạng có khả dụng không
     */
    public boolean isNetworkAvailable() {
        if (connectivityManager == null) {
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(
                    connectivityManager.getActiveNetwork());

            return capabilities != null && (
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
        } else {
            @SuppressWarnings("deprecation")
            android.net.NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();

            @SuppressWarnings("deprecation")
            boolean isConnected = activeNetworkInfo != null && activeNetworkInfo.isConnected();

            return isConnected;
        }
    }
}