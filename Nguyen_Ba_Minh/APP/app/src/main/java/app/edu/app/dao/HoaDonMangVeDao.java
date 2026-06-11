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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import app.edu.app.database.CoffeeDB;
import app.edu.app.interfaces.RealtimeUpdateCallback;
import app.edu.app.model.HoaDonMangVe;
import app.edu.app.utils.SyncUtils;
import app.edu.app.utils.XDate;

public class HoaDonMangVeDao extends FirebaseDAO {
    private static final String TABLE_NAME = "HoaDonMangVe";
    CoffeeDB coffeeDB;
    @SuppressLint("SimpleDateFormat")
    SimpleDateFormat spf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

    public HoaDonMangVeDao(Context context) {
        super(context);
        this.coffeeDB = new CoffeeDB(context);
    }

    public boolean insertHoaDonMangVe(HoaDonMangVe hoaDonMangVe) {
        SQLiteDatabase sqLiteDatabase = coffeeDB.getWritableDatabase();
        ContentValues values = new ContentValues();

        // If no ID is set, generate a random one
        if (hoaDonMangVe.getMaHoaDon() == 0) {
            hoaDonMangVe.setMaHoaDon(new Random().nextInt(999999 - 9999) + 9999);
        }

        values.put("maHoaDon", hoaDonMangVe.getMaHoaDon());
        values.put("maKhachHang", hoaDonMangVe.getMaKhachHang());
        values.put("gioVao", spf.format(hoaDonMangVe.getGioVao()));
        values.put("gioRa", spf.format(hoaDonMangVe.getGioRa()));
        values.put("trangThai", hoaDonMangVe.getTrangThai());
        values.put("ghiChu", hoaDonMangVe.getGhiChu());
        long check = sqLiteDatabase.insert("HOADONMANGVE", null, values);

        // If local insert successful and network available, sync to Firebase
        if (check != -1 && isNetworkAvailable()) {
            syncToFirebase(hoaDonMangVe);
        }

        return check != -1;
    }

    // Add this at class level
    private RealtimeUpdateCallback hoaDonMangVeCallback;

    /**
     * Start real-time synchronization for HoaDonMangVe table
     */
    public void startRealtimeSync() {
        hoaDonMangVeCallback = new RealtimeUpdateCallback() {
            @Override
            public void onDataChanged(DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    try {
                        Map<String, Object> map = (Map<String, Object>) snapshot.getValue();
                        if (map != null) {
                            HoaDonMangVe firebaseHDMV = SyncUtils.convertMapToHoaDonMangVe(map);

                            try {
                                HoaDonMangVe localHDMV = getByMaHoaDon(String.valueOf(firebaseHDMV.getMaHoaDon()));
                                // Update if needed
                                updateHoaDonMangVeFromFirebase(firebaseHDMV);
                            } catch (Exception e) {
                                // Item doesn't exist locally, insert it
                                insertHoaDonMangVeFromFirebase(firebaseHDMV);
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing Firebase HoaDonMangVe update", e);
                    }
                }
            }
        };

        setupRealtimeListener(TABLE_NAME, hoaDonMangVeCallback);
    }

    /**
     * Stop real-time synchronization
     */
    public void stopRealtimeSync() {
        removeRealtimeListener(TABLE_NAME);
    }

    // Firebase update methods
    private boolean updateHoaDonMangVeFromFirebase(HoaDonMangVe hoaDonMangVe) {
        SQLiteDatabase sqLiteDatabase = coffeeDB.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("maKhachHang", hoaDonMangVe.getMaKhachHang());
        values.put("gioVao", spf.format(hoaDonMangVe.getGioVao()));
        values.put("gioRa", spf.format(hoaDonMangVe.getGioRa()));
        values.put("trangThai", hoaDonMangVe.getTrangThai());
        values.put("ghiChu", hoaDonMangVe.getGhiChu());

        int check = sqLiteDatabase.update("HOADONMANGVE", values, "maHoaDon=?",
                new String[]{String.valueOf(hoaDonMangVe.getMaHoaDon())});

        if (check > 0) {
            Log.d(TAG, "Updated HoaDonMangVe from Firebase: " + hoaDonMangVe.getMaHoaDon());
        }

        return check > 0;
    }

    private boolean insertHoaDonMangVeFromFirebase(HoaDonMangVe hoaDonMangVe) {
        SQLiteDatabase sqLiteDatabase = coffeeDB.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("maHoaDon", hoaDonMangVe.getMaHoaDon());
        values.put("maKhachHang", hoaDonMangVe.getMaKhachHang());
        values.put("gioVao", spf.format(hoaDonMangVe.getGioVao()));
        values.put("gioRa", spf.format(hoaDonMangVe.getGioRa()));
        values.put("trangThai", hoaDonMangVe.getTrangThai());
        values.put("ghiChu", hoaDonMangVe.getGhiChu());

        long check = sqLiteDatabase.insert("HOADONMANGVE", null, values);

        if (check != -1) {
            Log.d(TAG, "Inserted HoaDonMangVe from Firebase: " + hoaDonMangVe.getMaHoaDon());
        }

        return check != -1;
    }

    //check hoa don chua duyet by manguoidung
    public boolean checkHoaDonChuaDuyet(String maKhachHang) {
        //or trangThai= CHUA_DUYET or trangThai= CHUA_XAC_NHAN
        String sql = "SELECT * FROM HOADONMANGVE WHERE maKhachHang=? AND trangThai=?";
        List<HoaDonMangVe> list = get(sql, maKhachHang, String.valueOf(HoaDonMangVe.CHUA_DUYET));
        return list.size() > 0;
    }

    @SuppressLint("Range")
    private List<HoaDonMangVe> get(String sql, String maKhachHang, String s) {
        ArrayList<HoaDonMangVe> list = new ArrayList<>();
        SQLiteDatabase sqLiteDatabase = coffeeDB.getWritableDatabase();
        Cursor cursor = sqLiteDatabase.rawQuery(sql, new String[]{maKhachHang, s});
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            do {
                HoaDonMangVe hoaDonMangVe = new HoaDonMangVe();
                hoaDonMangVe.setMaHoaDon(cursor.getInt(cursor.getColumnIndex("maHoaDon")));
                hoaDonMangVe.setMaKhachHang(cursor.getString(cursor.getColumnIndex("maKhachHang")));
                try {
                    hoaDonMangVe.setGioVao(XDate.toDateTime(cursor.getString(cursor.getColumnIndex("gioVao"))));
                    hoaDonMangVe.setGioRa(XDate.toDateTime(cursor.getString(cursor.getColumnIndex("gioRa"))));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                hoaDonMangVe.setTrangThai(cursor.getInt(cursor.getColumnIndex("trangThai")));
                hoaDonMangVe.setGhiChu(cursor.getString(cursor.getColumnIndex("ghiChu")));
                list.add(hoaDonMangVe);
                Log.i("TAG", hoaDonMangVe.toString());
            } while (cursor.moveToNext());
        }

        return list;
    }

    //check hoa don chua duyet by manguoidung
    public boolean checkHoaDonChuaXacNhan(String maKhachHang) {
        //or trangThai= CHUA_DUYET or trangThai= CHUA_XAC_NHAN
        String sql = "SELECT * FROM HOADONMANGVE WHERE maKhachHang=? AND trangThai=?";
        List<HoaDonMangVe> list = get(sql, maKhachHang, String.valueOf(HoaDonMangVe.CHUA_XAC_NHAN));
        return list.size() > 0;
    }

    public boolean deleteHoaDonMangVe(String maHoaDon) {
        SQLiteDatabase sqLiteDatabase = coffeeDB.getWritableDatabase();
        int check = sqLiteDatabase.delete("HOADONMANGVE", "maHoaDon=?", new String[]{maHoaDon});

        // If local delete successful and network available, delete from Firebase
        if (check > 0 && isNetworkAvailable()) {
            deleteFromFirebase(maHoaDon);
        }

        return check > 0;
    }

    public boolean updateHoaDonMangVe(HoaDonMangVe hoaDonMangVe) {
        SQLiteDatabase sqLiteDatabase = coffeeDB.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("maKhachHang", hoaDonMangVe.getMaKhachHang());
        values.put("gioVao", spf.format(hoaDonMangVe.getGioVao()));
        values.put("gioRa", spf.format(hoaDonMangVe.getGioRa()));
        values.put("trangThai", hoaDonMangVe.getTrangThai());
        values.put("ghiChu", hoaDonMangVe.getGhiChu());
        int check = sqLiteDatabase.update("HOADONMANGVE", values, "maHoaDon=?", new String[]{String.valueOf(hoaDonMangVe.getMaHoaDon())});

        // If local update successful and network available, sync to Firebase
        if (check > 0 && isNetworkAvailable()) {
            syncToFirebase(hoaDonMangVe);
        }

        return check > 0;
    }

    public HoaDonMangVe getByMaHoaDon(String maHoaDon) {
        String sqlGetByMaLoai = "SELECT * FROM HOADONMANGVE WHERE maHoaDon=?";
        List<HoaDonMangVe> list = get(sqlGetByMaLoai, maHoaDon);

        return list.get(0);
    }

    @SuppressLint("Range")
    private List<HoaDonMangVe> get(String sqlGetByMaLoai, String maHoaDon) {
        ArrayList<HoaDonMangVe> list = new ArrayList<>();
        SQLiteDatabase sqLiteDatabase = coffeeDB.getWritableDatabase();
        Cursor cursor = sqLiteDatabase.rawQuery(sqlGetByMaLoai, new String[]{maHoaDon});
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            do {
                HoaDonMangVe hoaDonMangVe = new HoaDonMangVe();
                hoaDonMangVe.setMaHoaDon(cursor.getInt(cursor.getColumnIndex("maHoaDon")));
                hoaDonMangVe.setMaKhachHang(cursor.getString(cursor.getColumnIndex("maKhachHang")));
                try {
                    hoaDonMangVe.setGioVao(XDate.toDateTime(cursor.getString(cursor.getColumnIndex("gioVao"))));
                    hoaDonMangVe.setGioRa(XDate.toDateTime(cursor.getString(cursor.getColumnIndex("gioRa"))));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                hoaDonMangVe.setTrangThai(cursor.getInt(cursor.getColumnIndex("trangThai")));
                hoaDonMangVe.setGhiChu(cursor.getString(cursor.getColumnIndex("ghiChu")));
                list.add(hoaDonMangVe);
                Log.i("TAG", hoaDonMangVe.toString());
            } while (cursor.moveToNext());
        }

        return list;
    }

    //getAll hoa don by manguoidung
    public List<HoaDonMangVe> getAllByMaKhachHang(String maKhachHang) {
        //trạng thái đã duyệt và chưa duyệt
        String sql = "SELECT * FROM HOADONMANGVE WHERE maKhachHang=? AND trangThai=?";
        return get(sql, maKhachHang,String.valueOf(HoaDonMangVe.DA_DUYET));
    }

    //get one hoa don by manguoidung and trangthai 1
    public HoaDonMangVe getByMaHoaDonVaTrangThai(String maKhachHang) {
        //khôgn lấy trạng thái hủy hoá đơn và đã duyệt
        String sql = "SELECT * FROM HOADONMANGVE WHERE maKhachHang=? AND trangThai=?";
        List<HoaDonMangVe> list = get(sql, maKhachHang, String.valueOf(HoaDonMangVe.CHUA_DUYET));
        if(list.size()==0){
            list = get(sql, maKhachHang, String.valueOf(HoaDonMangVe.CHUA_XAC_NHAN));
        }
        return list.get(0);
    }

    public List<HoaDonMangVe> getByTrangThai(int trangThai) {
        String sql = "SELECT * FROM HOADONMANGVE WHERE trangThai=?";
        return get(sql, String.valueOf(trangThai));
    }

    @SuppressLint("Range")
    public int getGiaTien(int maHoaDon) {
        SQLiteDatabase sqLiteDatabase = coffeeDB.getReadableDatabase();
        String sql = "SELECT SUM(giaTien) as DoanhThu FROM HOADONCHITIET WHERE maHoaDon=?";
        ArrayList<Integer> list = new ArrayList<>();
        @SuppressLint("Recycle")
        Cursor cursor = sqLiteDatabase.rawQuery(sql, new String[]{String.valueOf(maHoaDon)});
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            do {
                try {
                    list.add(cursor.getInt(cursor.getColumnIndex("DoanhThu")));
                } catch (Exception e) {
                    list.add(0);
                }
            } while (cursor.moveToNext());
        }
        return list.get(0);
    }

    // Sync a HoaDonMangVe object to Firebase
    private void syncToFirebase(HoaDonMangVe hoaDonMangVe) {
        Map<String, Object> hoaDonMangVeMap = SyncUtils.convertHoaDonMangVeToMap(hoaDonMangVe);

        dbRef.child(TABLE_NAME).child(String.valueOf(hoaDonMangVe.getMaHoaDon()))
                .setValue(hoaDonMangVeMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        logSuccessfulSync("Sync", String.valueOf(hoaDonMangVe.getMaHoaDon()));
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        handleSyncError("Sync", String.valueOf(hoaDonMangVe.getMaHoaDon()), e);
                    }
                });
    }

    // Delete a HoaDonMangVe from Firebase
    private void deleteFromFirebase(String maHoaDon) {
        dbRef.child(TABLE_NAME).child(maHoaDon)
                .removeValue()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        logSuccessfulSync("Delete", maHoaDon);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        handleSyncError("Delete", maHoaDon, e);
                    }
                });
    }

    @SuppressLint("Range")
    public List<HoaDonMangVe> getAll() {
        ArrayList<HoaDonMangVe> list = new ArrayList<>();
        SQLiteDatabase sqLiteDatabase = coffeeDB.getWritableDatabase();
        Cursor cursor = sqLiteDatabase.rawQuery("SELECT * FROM HOADONMANGVE", null);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            do {
                HoaDonMangVe hoaDonMangVe = new HoaDonMangVe();
                hoaDonMangVe.setMaHoaDon(cursor.getInt(cursor.getColumnIndex("maHoaDon")));
                hoaDonMangVe.setMaKhachHang(cursor.getString(cursor.getColumnIndex("maKhachHang")));
                try {
                    hoaDonMangVe.setGioVao(XDate.toDateTime(cursor.getString(cursor.getColumnIndex("gioVao"))));
                    hoaDonMangVe.setGioRa(XDate.toDateTime(cursor.getString(cursor.getColumnIndex("gioRa"))));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                hoaDonMangVe.setTrangThai(cursor.getInt(cursor.getColumnIndex("trangThai")));
                hoaDonMangVe.setGhiChu(cursor.getString(cursor.getColumnIndex("ghiChu")));
                list.add(hoaDonMangVe);
                Log.i("TAG", hoaDonMangVe.toString());
            } while (cursor.moveToNext());
        }

        return list;
    }
}
