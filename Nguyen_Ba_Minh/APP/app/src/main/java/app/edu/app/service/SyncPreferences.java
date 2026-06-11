package app.edu.app.service;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashMap;
import java.util.Map;

public class SyncPreferences {
    private static final String PREF_NAME = "sync_preferences";
    private static final String LAST_SYNC_TIME = "last_sync_time";

    // Table-specific sync timestamps
    private static final String LAST_SYNC_BAN = "last_sync_ban";
    private static final String LAST_SYNC_HANGHOA = "last_sync_hanghoa";
    private static final String LAST_SYNC_HOADON = "last_sync_hoadon";
    private static final String LAST_SYNC_HOADONCHITIET = "last_sync_hoadonchitiet";
    private static final String LAST_SYNC_HOADONMANGVE = "last_sync_hoadonmangve";
    private static final String LAST_SYNC_LOAIHANG = "last_sync_loaihang";
    private static final String LAST_SYNC_NGUOIDUNG = "last_sync_nguoidung";
    private static final String LAST_SYNC_THONGBAO = "last_sync_thongbao";

    // Sync status flags
    private static final String PENDING_CHANGES = "pending_changes";
    private static final String IS_FIRST_SYNC = "is_first_sync";

    private SharedPreferences preferences;

    public SyncPreferences(Context context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // Get all timestamp information
    public Map<String, Long> getAllSyncTimestamps() {
        Map<String, Long> timestamps = new HashMap<>();
        timestamps.put(LAST_SYNC_BAN, getBanLastSyncTime());
        timestamps.put(LAST_SYNC_HANGHOA, getHangHoaLastSyncTime());
        timestamps.put(LAST_SYNC_HOADON, getHoaDonLastSyncTime());
        timestamps.put(LAST_SYNC_HOADONCHITIET, getHoaDonChiTietLastSyncTime());
        timestamps.put(LAST_SYNC_HOADONMANGVE, getHoaDonMangVeLastSyncTime());
        timestamps.put(LAST_SYNC_LOAIHANG, getLoaiHangLastSyncTime());
        timestamps.put(LAST_SYNC_NGUOIDUNG, getNguoiDungLastSyncTime());
        timestamps.put(LAST_SYNC_THONGBAO, getThongBaoLastSyncTime());
        return timestamps;
    }

    // Update the last sync timestamp for all tables
    public void updateLastSyncTimestamp() {
        long currentTime = System.currentTimeMillis();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putLong(LAST_SYNC_TIME, currentTime);
        editor.putLong(LAST_SYNC_BAN, currentTime);
        editor.putLong(LAST_SYNC_HANGHOA, currentTime);
        editor.putLong(LAST_SYNC_HOADON, currentTime);
        editor.putLong(LAST_SYNC_HOADONCHITIET, currentTime);
        editor.putLong(LAST_SYNC_HOADONMANGVE, currentTime);
        editor.putLong(LAST_SYNC_LOAIHANG, currentTime);
        editor.putLong(LAST_SYNC_NGUOIDUNG, currentTime);
        editor.putLong(LAST_SYNC_THONGBAO, currentTime);
        editor.putBoolean(PENDING_CHANGES, false);
        editor.putBoolean(IS_FIRST_SYNC, false);
        editor.apply();
    }

    // Update timestamps for specific tables
    public void updateTableSyncTimestamp(String tableName) {
        long currentTime = System.currentTimeMillis();
        SharedPreferences.Editor editor = preferences.edit();

        switch (tableName) {
            case "Ban":
                editor.putLong(LAST_SYNC_BAN, currentTime);
                break;
            case "HangHoa":
                editor.putLong(LAST_SYNC_HANGHOA, currentTime);
                break;
            case "HoaDon":
                editor.putLong(LAST_SYNC_HOADON, currentTime);
                break;
            case "HoaDonChiTiet":
                editor.putLong(LAST_SYNC_HOADONCHITIET, currentTime);
                break;
            case "HoaDonMangVe":
                editor.putLong(LAST_SYNC_HOADONMANGVE, currentTime);
                break;
            case "LoaiHang":
                editor.putLong(LAST_SYNC_LOAIHANG, currentTime);
                break;
            case "NguoiDung":
                editor.putLong(LAST_SYNC_NGUOIDUNG, currentTime);
                break;
            case "ThongBao":
                editor.putLong(LAST_SYNC_THONGBAO, currentTime);
                break;
        }

        editor.apply();
    }

    // Flag that changes are pending for next sync
    public void setPendingChanges(boolean hasPendingChanges) {
        preferences.edit().putBoolean(PENDING_CHANGES, hasPendingChanges).apply();
    }

    // Check if there are pending changes
    public boolean hasPendingChanges() {
        return preferences.getBoolean(PENDING_CHANGES, false);
    }

    // Check if this is the first sync
    public boolean isFirstSync() {
        return preferences.getBoolean(IS_FIRST_SYNC, true);
    }

    // Get last sync time for all data
    public long getLastSyncTime() {
        return preferences.getLong(LAST_SYNC_TIME, 0);
    }

    // Get table-specific last sync times
    public long getBanLastSyncTime() {
        return preferences.getLong(LAST_SYNC_BAN, 0);
    }

    public long getHangHoaLastSyncTime() {
        return preferences.getLong(LAST_SYNC_HANGHOA, 0);
    }

    public long getHoaDonLastSyncTime() {
        return preferences.getLong(LAST_SYNC_HOADON, 0);
    }

    public long getHoaDonChiTietLastSyncTime() {
        return preferences.getLong(LAST_SYNC_HOADONCHITIET, 0);
    }

    public long getHoaDonMangVeLastSyncTime() {
        return preferences.getLong(LAST_SYNC_HOADONMANGVE, 0);
    }

    public long getLoaiHangLastSyncTime() {
        return preferences.getLong(LAST_SYNC_LOAIHANG, 0);
    }

    public long getNguoiDungLastSyncTime() {
        return preferences.getLong(LAST_SYNC_NGUOIDUNG, 0);
    }

    public long getThongBaoLastSyncTime() {
        return preferences.getLong(LAST_SYNC_THONGBAO, 0);
    }
}