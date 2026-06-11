package app.edu.app.service;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import app.edu.app.dao.BanDAO;
import app.edu.app.dao.HangHoaDAO;
import app.edu.app.dao.HoaDonChiTietDAO;
import app.edu.app.dao.HoaDonDAO;
import app.edu.app.dao.HoaDonMangVeDao;
import app.edu.app.dao.LoaiHangDAO;
import app.edu.app.dao.NguoiDungDAO;
import app.edu.app.dao.ThongBaoDAO;
import app.edu.app.model.Ban;
import app.edu.app.model.HangHoa;
import app.edu.app.model.HoaDon;
import app.edu.app.model.HoaDonChiTiet;
import app.edu.app.model.HoaDonMangVe;
import app.edu.app.model.LoaiHang;
import app.edu.app.model.NguoiDung;
import app.edu.app.model.ThongBao;
import app.edu.app.utils.SyncUtils;

public class FirebaseSyncService extends Worker {
    private static final String TAG = "FirebaseSyncService";

    private Context context;
    private FirebaseDatabase database;
    private DatabaseReference dbRef;

    // DAOs
    private BanDAO banDAO;
    private HangHoaDAO hangHoaDAO;
    private HoaDonDAO hoaDonDAO;
    private HoaDonChiTietDAO hoaDonChiTietDAO;
    private HoaDonMangVeDao hoaDonMangVeDao;
    private LoaiHangDAO loaiHangDAO;
    private NguoiDungDAO nguoiDungDAO;
    private ThongBaoDAO thongBaoDAO;

    // Preferences to track last sync
    private SyncPreferences syncPreferences;

    public FirebaseSyncService(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.context = context;

        // Initialize Firebase
        database = FirebaseDatabase.getInstance();
        dbRef = database.getReference();

        // Initialize DAOs
        banDAO = new BanDAO(context);
        hangHoaDAO = new HangHoaDAO(context);
        hoaDonDAO = new HoaDonDAO(context);
        hoaDonChiTietDAO = new HoaDonChiTietDAO(context);
        hoaDonMangVeDao = new HoaDonMangVeDao(context);
        loaiHangDAO = new LoaiHangDAO(context);
        nguoiDungDAO = new NguoiDungDAO(context);
        thongBaoDAO = new ThongBaoDAO(context);

        // Initialize preferences
        syncPreferences = new SyncPreferences(context);
    }

    @NonNull
    @Override
    public Result doWork() {
        if (!isNetworkAvailable()) {
            Log.d(TAG, "No network connection available, skipping sync");
            return Result.retry();
        }

        try {
            Log.d(TAG, "Starting synchronization process");

            // Upload local changes to Firebase
            uploadLocalChangesToFirebase();

            // Download changes from Firebase
            downloadChangesFromFirebase();

            // Update last sync timestamp
            syncPreferences.updateLastSyncTimestamp();

            Log.d(TAG, "Synchronization completed successfully");
            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Synchronization failed", e);
            return Result.failure();
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }

    // Upload local SQLite data to Firebase
    private void uploadLocalChangesToFirebase() {
        Log.d(TAG, "Uploading local changes to Firebase");

        // Upload tables that have changed since last sync
        uploadBanData();
        uploadHangHoaData();
        uploadHoaDonData();
        uploadHoaDonChiTietData();
        uploadHoaDonMangVeData();
        uploadLoaiHangData();
        uploadNguoiDungData();
        uploadThongBaoData();
    }

    private void uploadBanData() {
        ArrayList<Ban> banList = banDAO.getAll();
        if (!banList.isEmpty()) {
            Map<String, Object> banUpdates = new HashMap<>();
            for (Ban ban : banList) {
                banUpdates.put("Ban/" + ban.getMaBan(), SyncUtils.convertBanToMap(ban));
            }
            dbRef.updateChildren(banUpdates);
        }
    }

    private void uploadHangHoaData() {
        ArrayList<HangHoa> hangHoaList = hangHoaDAO.getAll();
        if (!hangHoaList.isEmpty()) {
            Map<String, Object> hangHoaUpdates = new HashMap<>();
            for (HangHoa hangHoa : hangHoaList) {
                hangHoaUpdates.put("HangHoa/" + hangHoa.getMaHangHoa(), SyncUtils.convertHangHoaToMap(hangHoa));
            }
            dbRef.updateChildren(hangHoaUpdates);
        }
    }

    private void uploadHoaDonData() {
        ArrayList<HoaDon> hoaDonList = hoaDonDAO.getAll();
        if (!hoaDonList.isEmpty()) {
            Map<String, Object> hoaDonUpdates = new HashMap<>();
            for (HoaDon hoaDon : hoaDonList) {
                hoaDonUpdates.put("HoaDon/" + hoaDon.getMaHoaDon(), SyncUtils.convertHoaDonToMap(hoaDon));
            }
            dbRef.updateChildren(hoaDonUpdates);
        }
    }

    private void uploadHoaDonChiTietData() {
        ArrayList<HoaDonChiTiet> hoaDonChiTietList = hoaDonChiTietDAO.getAll();
        if (!hoaDonChiTietList.isEmpty()) {
            Map<String, Object> hoaDonChiTietUpdates = new HashMap<>();
            for (HoaDonChiTiet hoaDonChiTiet : hoaDonChiTietList) {
                hoaDonChiTietUpdates.put("HoaDonChiTiet/" + hoaDonChiTiet.getMaHDCT(),
                        SyncUtils.convertHoaDonChiTietToMap(hoaDonChiTiet));
            }
            dbRef.updateChildren(hoaDonChiTietUpdates);
        }
    }

    private void uploadHoaDonMangVeData() {
        List<HoaDonMangVe> hoaDonMangVeList = hoaDonMangVeDao.getAll();
        if (!hoaDonMangVeList.isEmpty()) {
            Map<String, Object> hoaDonMangVeUpdates = new HashMap<>();
            for (HoaDonMangVe hoaDonMangVe : hoaDonMangVeList) {
                hoaDonMangVeUpdates.put("HoaDonMangVe/" + hoaDonMangVe.getMaHoaDon(),
                        SyncUtils.convertHoaDonMangVeToMap(hoaDonMangVe));
            }
            dbRef.updateChildren(hoaDonMangVeUpdates);
        }
    }

    private void uploadLoaiHangData() {
        ArrayList<LoaiHang> loaiHangList = loaiHangDAO.getAll();
        if (!loaiHangList.isEmpty()) {
            Map<String, Object> loaiHangUpdates = new HashMap<>();
            for (LoaiHang loaiHang : loaiHangList) {
                loaiHangUpdates.put("LoaiHang/" + loaiHang.getMaLoai(), SyncUtils.convertLoaiHangToMap(loaiHang));
            }
            dbRef.updateChildren(loaiHangUpdates);
        }
    }

    private void uploadNguoiDungData() {
        ArrayList<NguoiDung> nguoiDungList = nguoiDungDAO.getAll();
        if (!nguoiDungList.isEmpty()) {
            Map<String, Object> nguoiDungUpdates = new HashMap<>();
            for (NguoiDung nguoiDung : nguoiDungList) {
                nguoiDungUpdates.put("NguoiDung/" + nguoiDung.getMaNguoiDung(),
                        SyncUtils.convertNguoiDungToMap(nguoiDung));
            }
            dbRef.updateChildren(nguoiDungUpdates);
        }
    }

    private void uploadThongBaoData() {
        ArrayList<ThongBao> thongBaoList = thongBaoDAO.getAll();
        if (!thongBaoList.isEmpty()) {
            Map<String, Object> thongBaoUpdates = new HashMap<>();
            for (ThongBao thongBao : thongBaoList) {
                thongBaoUpdates.put("ThongBao/" + thongBao.getMaThongBao(),
                        SyncUtils.convertThongBaoToMap(thongBao));
            }
            dbRef.updateChildren(thongBaoUpdates);
        }
    }

    // Download Firebase data to local SQLite

    // Update the download methods in FirebaseSyncService.java

    // Update the download methods in FirebaseSyncService.java

    private void downloadChangesFromFirebase() {
        Log.d(TAG, "Downloading changes from Firebase");

        final CountDownLatch latch = new CountDownLatch(8); // For 8 tables

        // Ban table
        dbRef.child("Ban").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    try {
                        Map<String, Object> map = (Map<String, Object>) snapshot.getValue();
                        if (map != null) {
                            Ban ban = new Ban();
                            ban.setMaBan(((Long) map.get("maBan")).intValue());
                            ban.setTrangThai(((Long) map.get("trangThai")).intValue());

                            try {
                                // Check if ban exists in local database
                                banDAO.getByMaBan(String.valueOf(ban.getMaBan()));
                                // If exists, update
                                banDAO.updateBan(ban);
                            } catch (Exception e) {
                                // If doesn't exist, insert
                                banDAO.insertBan(ban);
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing Firebase Ban data", e);
                    }
                }
                latch.countDown();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Firebase Ban sync failed", error.toException());
                latch.countDown();
            }
        });

        // HangHoa table
        dbRef.child("HangHoa").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    try {
                        Map<String, Object> map = (Map<String, Object>) snapshot.getValue();
                        if (map != null) {
                            HangHoa hangHoa = SyncUtils.convertMapToHangHoa(map);

                            try {
                                hangHoaDAO.getByMaHangHoa(String.valueOf(hangHoa.getMaHangHoa()));
                                hangHoaDAO.updateHangHoa(hangHoa);
                            } catch (Exception e) {
                                hangHoaDAO.insertHangHoa(hangHoa);
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing Firebase HangHoa data", e);
                    }
                }
                latch.countDown();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Firebase HangHoa sync failed", error.toException());
                latch.countDown();
            }
        });

        // HoaDon table
        dbRef.child("HoaDon").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    try {
                        Map<String, Object> map = (Map<String, Object>) snapshot.getValue();
                        if (map != null) {
                            HoaDon hoaDon = SyncUtils.convertMapToHoaDon(map);

                            try {
                                hoaDonDAO.getByMaHoaDon(String.valueOf(hoaDon.getMaHoaDon()));
                                hoaDonDAO.updateHoaDon(hoaDon);
                            } catch (Exception e) {
                                hoaDonDAO.insertHoaDon(hoaDon);
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing Firebase HoaDon data", e);
                    }
                }
                latch.countDown();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Firebase HoaDon sync failed", error.toException());
                latch.countDown();
            }
        });

        // HoaDonChiTiet table
        dbRef.child("HoaDonChiTiet").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    try {
                        Map<String, Object> map = (Map<String, Object>) snapshot.getValue();
                        if (map != null) {
                            HoaDonChiTiet hoaDonChiTiet = SyncUtils.convertMapToHoaDonChiTiet(map);

                            try {
                                hoaDonChiTietDAO.getByMaHDCT(String.valueOf(hoaDonChiTiet.getMaHDCT()));
                                hoaDonChiTietDAO.updateHoaDonChiTiet(hoaDonChiTiet);
                            } catch (Exception e) {
                                hoaDonChiTietDAO.insertHoaDonChiTiet(hoaDonChiTiet);
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing Firebase HoaDonChiTiet data", e);
                    }
                }
                latch.countDown();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Firebase HoaDonChiTiet sync failed", error.toException());
                latch.countDown();
            }
        });

        // HoaDonMangVe table
        dbRef.child("HoaDonMangVe").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    try {
                        Map<String, Object> map = (Map<String, Object>) snapshot.getValue();
                        if (map != null) {
                            HoaDonMangVe hoaDonMangVe = SyncUtils.convertMapToHoaDonMangVe(map);

                            try {
                                hoaDonMangVeDao.getByMaHoaDon(String.valueOf(hoaDonMangVe.getMaHoaDon()));
                                hoaDonMangVeDao.updateHoaDonMangVe(hoaDonMangVe);
                            } catch (Exception e) {
                                hoaDonMangVeDao.insertHoaDonMangVe(hoaDonMangVe);
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing Firebase HoaDonMangVe data", e);
                    }
                }
                latch.countDown();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Firebase HoaDonMangVe sync failed", error.toException());
                latch.countDown();
            }
        });

        // LoaiHang table
        dbRef.child("LoaiHang").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    try {
                        Map<String, Object> map = (Map<String, Object>) snapshot.getValue();
                        if (map != null) {
                            LoaiHang loaiHang = SyncUtils.convertMapToLoaiHang(map);

                            try {
                                loaiHangDAO.getByMaLoai(String.valueOf(loaiHang.getMaLoai()));
                                loaiHangDAO.updateLoaiHang(loaiHang);
                            } catch (Exception e) {
                                loaiHangDAO.insertLoaiHang(loaiHang);
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing Firebase LoaiHang data", e);
                    }
                }
                latch.countDown();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Firebase LoaiHang sync failed", error.toException());
                latch.countDown();
            }
        });

        // NguoiDung table
        dbRef.child("NguoiDung").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    try {
                        Map<String, Object> map = (Map<String, Object>) snapshot.getValue();
                        if (map != null) {
                            NguoiDung nguoiDung = SyncUtils.convertMapToNguoiDung(map);

                            try {
                                nguoiDungDAO.getByMaNguoiDung(nguoiDung.getMaNguoiDung());
                                nguoiDungDAO.updateNguoiDung(nguoiDung);
                            } catch (Exception e) {
                                nguoiDungDAO.insertNguoiDung(nguoiDung);
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing Firebase NguoiDung data", e);
                    }
                }
                latch.countDown();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Firebase NguoiDung sync failed", error.toException());
                latch.countDown();
            }
        });

        // ThongBao table
        dbRef.child("ThongBao").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    try {
                        Map<String, Object> map = (Map<String, Object>) snapshot.getValue();
                        if (map != null) {
                            ThongBao thongBao = SyncUtils.convertMapToThongBao(map);

                            try {
                                thongBaoDAO.getByMaThongBao(String.valueOf(thongBao.getMaThongBao()));
                                thongBaoDAO.updateThongBao(thongBao);
                            } catch (Exception e) {
                                thongBaoDAO.insertThongBao(thongBao);
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing Firebase ThongBao data", e);
                    }
                }
                latch.countDown();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Firebase ThongBao sync failed", error.toException());
                latch.countDown();
            }
        });

        try {
            // Wait for all sync operations to complete
            latch.await();
        } catch (InterruptedException e) {
            Log.e(TAG, "Sync interrupted", e);
        }
    }
}