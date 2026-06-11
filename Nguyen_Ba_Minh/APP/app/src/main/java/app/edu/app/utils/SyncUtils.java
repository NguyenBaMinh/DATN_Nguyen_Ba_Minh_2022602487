package app.edu.app.utils;

import android.util.Base64;
import android.util.Log;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import app.edu.app.model.Ban;
import app.edu.app.model.DatBan;
import app.edu.app.model.HangHoa;
import app.edu.app.model.HoaDon;
import app.edu.app.model.HoaDonChiTiet;
import app.edu.app.model.HoaDonMangVe;
import app.edu.app.model.LoaiHang;
import app.edu.app.model.NguoiDung;
import app.edu.app.model.ThongBao;

public class SyncUtils {
    private static final String TAG = "SyncUtils";
    private static final ExecutorService executor = Executors.newFixedThreadPool(3);

    // Convert blob data to Base64 string for Firebase
    public static String blobToBase64(byte[] blob) {
        if (blob == null) {
            return null;
        }
        return Base64.encodeToString(blob, Base64.DEFAULT);
    }

    // Convert Base64 string back to blob for SQLite
    public static byte[] base64ToBlob(String base64) {
        if (base64 == null) {
            return null;
        }
        return Base64.decode(base64, Base64.DEFAULT);
    }

    // Convert Ban object to Map for Firebase
    public static Map<String, Object> convertBanToMap(Ban ban) {
        Map<String, Object> banMap = new HashMap<>();
        banMap.put("maBan", ban.getMaBan());
        banMap.put("trangThai", ban.getTrangThai());
        banMap.put("lastModified", System.currentTimeMillis());
        return banMap;
    }

    // Convert HangHoa object to Map for Firebase - Async version with Storage
    public static void convertHangHoaToMapAsync(HangHoa hangHoa, final OnMapConversionListener listener) {
        executor.execute(() -> {
            try {
                Map<String, Object> hangHoaMap = new HashMap<>();
                hangHoaMap.put("maHangHoa", hangHoa.getMaHangHoa());
                hangHoaMap.put("tenHangHoa", hangHoa.getTenHangHoa());
                hangHoaMap.put("giaTien", hangHoa.getGiaTien());
                hangHoaMap.put("maLoai", hangHoa.getMaLoai());
                hangHoaMap.put("trangThai", hangHoa.getTrangThai());
                hangHoaMap.put("lastModified", System.currentTimeMillis());
                hangHoaMap.put("hasImage", hangHoa.getHinhAnh() != null && hangHoa.getHinhAnh().length > 0);

                // Upload image to Storage if present
                byte[] hinhAnh = hangHoa.getHinhAnh();
                if (hinhAnh != null && hinhAnh.length > 0) {
                    byte[] compressedImage = StorageUtils.compressImage(hinhAnh);
                    String path = "hanghoa/" + hangHoa.getMaHangHoa() + ".jpg";

                    StorageUtils.uploadImage(compressedImage, path, new StorageUtils.OnUploadCompleteListener() {
                        @Override
                        public void onSuccess(String downloadUrl) {
                            hangHoaMap.put("hinhAnhUrl", downloadUrl);
                            listener.onSuccess(hangHoaMap);
                        }

                        @Override
                        public void onFailure(Exception e) {
                            hangHoaMap.put("hinhAnhUrl", null);
                            listener.onSuccess(hangHoaMap); // Still return map even without image
                        }
                    });
                } else {
                    hangHoaMap.put("hinhAnhUrl", null);
                    listener.onSuccess(hangHoaMap);
                }
            } catch (Exception e) {
                listener.onFailure(e);
            }
        });
    }

    // Synchronous version for HangHoa
    public static Map<String, Object> convertHangHoaToMap(HangHoa hangHoa) {
        Map<String, Object> hangHoaMap = new HashMap<>();
        hangHoaMap.put("maHangHoa", hangHoa.getMaHangHoa());
        hangHoaMap.put("tenHangHoa", hangHoa.getTenHangHoa());
        hangHoaMap.put("giaTien", hangHoa.getGiaTien());
        hangHoaMap.put("maLoai", hangHoa.getMaLoai());
        hangHoaMap.put("trangThai", hangHoa.getTrangThai());
        hangHoaMap.put("lastModified", System.currentTimeMillis());
        hangHoaMap.put("hasImage", hangHoa.getHinhAnh() != null && hangHoa.getHinhAnh().length > 0);

        // Don't include the image data in Firebase Database
        // Images should be handled separately with Storage
        return hangHoaMap;
    }

    // Convert Map back to HangHoa
    public static HangHoa convertMapToHangHoa(Map<String, Object> map) {
        HangHoa hangHoa = new HangHoa();

        try {
            hangHoa.setMaHangHoa(((Long) map.get("maHangHoa")).intValue());
            hangHoa.setTenHangHoa((String) map.get("tenHangHoa"));

            // Handle image URL for downloading later
            String imageUrl = (String) map.get("hinhAnhUrl");
            if (imageUrl != null && !imageUrl.isEmpty()) {
                // Store URL in cache for later download
                ImageCache.addUrlToCache(String.valueOf(hangHoa.getMaHangHoa()), imageUrl);
            }

            hangHoa.setGiaTien(((Long) map.get("giaTien")).intValue());
            hangHoa.setMaLoai(((Long) map.get("maLoai")).intValue());
            hangHoa.setTrangThai(((Long) map.get("trangThai")).intValue());
        } catch (Exception e) {
            Log.e(TAG, "Error converting map to HangHoa", e);
        }

        return hangHoa;
    }

    // Convert LoaiHang object to Map for Firebase - Async version with Storage
    public static void convertLoaiHangToMapAsync(LoaiHang loaiHang, final OnMapConversionListener listener) {
        executor.execute(() -> {
            try {
                Map<String, Object> loaiHangMap = new HashMap<>();
                loaiHangMap.put("maLoai", loaiHang.getMaLoai());
                loaiHangMap.put("tenLoai", loaiHang.getTenLoai());
                loaiHangMap.put("lastModified", System.currentTimeMillis());
                loaiHangMap.put("hasImage", loaiHang.getHinhAnh() != null && loaiHang.getHinhAnh().length > 0);

                byte[] hinhAnh = loaiHang.getHinhAnh();
                if (hinhAnh != null && hinhAnh.length > 0) {
                    byte[] compressedImage = StorageUtils.compressImage(hinhAnh);
                    String path = "loaihang/" + loaiHang.getMaLoai() + ".jpg";

                    StorageUtils.uploadImage(compressedImage, path, new StorageUtils.OnUploadCompleteListener() {
                        @Override
                        public void onSuccess(String downloadUrl) {
                            loaiHangMap.put("hinhAnhUrl", downloadUrl);
                            listener.onSuccess(loaiHangMap);
                        }

                        @Override
                        public void onFailure(Exception e) {
                            loaiHangMap.put("hinhAnhUrl", null);
                            listener.onSuccess(loaiHangMap);
                        }
                    });
                } else {
                    loaiHangMap.put("hinhAnhUrl", null);
                    listener.onSuccess(loaiHangMap);
                }
            } catch (Exception e) {
                listener.onFailure(e);
            }
        });
    }

    // Synchronous version for LoaiHang
    public static Map<String, Object> convertLoaiHangToMap(LoaiHang loaiHang) {
        Map<String, Object> loaiHangMap = new HashMap<>();
        loaiHangMap.put("maLoai", loaiHang.getMaLoai());
        loaiHangMap.put("tenLoai", loaiHang.getTenLoai());
        loaiHangMap.put("lastModified", System.currentTimeMillis());
        loaiHangMap.put("hasImage", loaiHang.getHinhAnh() != null && loaiHang.getHinhAnh().length > 0);

        // Don't include the image data in Firebase Database
        return loaiHangMap;
    }

    // Convert Map back to LoaiHang
    public static LoaiHang convertMapToLoaiHang(Map<String, Object> map) {
        LoaiHang loaiHang = new LoaiHang();
        try {
            loaiHang.setMaLoai(((Long) map.get("maLoai")).intValue());
            loaiHang.setTenLoai((String) map.get("tenLoai"));

            // Handle image URL for downloading later
            String imageUrl = (String) map.get("hinhAnhUrl");
            if (imageUrl != null && !imageUrl.isEmpty()) {
                // Store URL in cache for later download
                ImageCache.addUrlToCache("loaihang_" + loaiHang.getMaLoai(), imageUrl);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error converting map to LoaiHang", e);
        }

        return loaiHang;
    }

    // Convert NguoiDung object to Map for Firebase - Async version with Storage
    public static void convertNguoiDungToMapAsync(NguoiDung nguoiDung, final OnMapConversionListener listener) {
        executor.execute(() -> {
            try {
                Map<String, Object> nguoiDungMap = new HashMap<>();
                nguoiDungMap.put("maNguoiDung", nguoiDung.getMaNguoiDung());
                nguoiDungMap.put("hoVaTen", nguoiDung.getHoVaTen());
                nguoiDungMap.put("ngaySinh", XDate.toStringDate(nguoiDung.getNgaySinh()));
                nguoiDungMap.put("email", nguoiDung.getEmail());
                nguoiDungMap.put("chucVu", nguoiDung.getChucVu());
                nguoiDungMap.put("gioiTinh", nguoiDung.getGioiTinh());
                nguoiDungMap.put("matKhau", nguoiDung.getMatKhau());
                nguoiDungMap.put("lastModified", System.currentTimeMillis());
                nguoiDungMap.put("hasImage", nguoiDung.getHinhAnh() != null && nguoiDung.getHinhAnh().length > 0);

                byte[] hinhAnh = nguoiDung.getHinhAnh();
                if (hinhAnh != null && hinhAnh.length > 0) {
                    byte[] compressedImage = StorageUtils.compressImage(hinhAnh);
                    String path = "nguoidung/" + nguoiDung.getMaNguoiDung() + ".jpg";

                    StorageUtils.uploadImage(compressedImage, path, new StorageUtils.OnUploadCompleteListener() {
                        @Override
                        public void onSuccess(String downloadUrl) {
                            nguoiDungMap.put("hinhAnhUrl", downloadUrl);
                            listener.onSuccess(nguoiDungMap);
                        }

                        @Override
                        public void onFailure(Exception e) {
                            nguoiDungMap.put("hinhAnhUrl", null);
                            listener.onSuccess(nguoiDungMap);
                        }
                    });
                } else {
                    nguoiDungMap.put("hinhAnhUrl", null);
                    listener.onSuccess(nguoiDungMap);
                }
            } catch (Exception e) {
                listener.onFailure(e);
            }
        });
    }

    // Synchronous version for NguoiDung
    public static Map<String, Object> convertNguoiDungToMap(NguoiDung nguoiDung) {
        Map<String, Object> nguoiDungMap = new HashMap<>();
        nguoiDungMap.put("maNguoiDung", nguoiDung.getMaNguoiDung());
        nguoiDungMap.put("hoVaTen", nguoiDung.getHoVaTen());
        nguoiDungMap.put("ngaySinh", XDate.toStringDate(nguoiDung.getNgaySinh()));
        nguoiDungMap.put("email", nguoiDung.getEmail());
        nguoiDungMap.put("chucVu", nguoiDung.getChucVu());
        nguoiDungMap.put("gioiTinh", nguoiDung.getGioiTinh());
        nguoiDungMap.put("matKhau", nguoiDung.getMatKhau());
        nguoiDungMap.put("lastModified", System.currentTimeMillis());
        nguoiDungMap.put("hasImage", nguoiDung.getHinhAnh() != null && nguoiDung.getHinhAnh().length > 0);

        // Don't include the image data in Firebase Database
        return nguoiDungMap;
    }

    // Convert Map back to NguoiDung
    public static NguoiDung convertMapToNguoiDung(Map<String, Object> map) {
        NguoiDung nguoiDung = new NguoiDung();
        try {
            nguoiDung.setMaNguoiDung((String) map.get("maNguoiDung"));
            nguoiDung.setHoVaTen((String) map.get("hoVaTen"));

            // Handle image URL for downloading later
            String imageUrl = (String) map.get("hinhAnhUrl");
            if (imageUrl != null && !imageUrl.isEmpty()) {
                // Store URL in cache for later download
                ImageCache.addUrlToCache("nguoidung_" + nguoiDung.getMaNguoiDung(), imageUrl);
            }

            try {
                nguoiDung.setNgaySinh(XDate.toDate((String) map.get("ngaySinh")));
            } catch (ParseException e) {
                e.printStackTrace();
            }

            nguoiDung.setEmail((String) map.get("email"));
            nguoiDung.setChucVu((String) map.get("chucVu"));
            nguoiDung.setGioiTinh((String) map.get("gioiTinh"));
            nguoiDung.setMatKhau((String) map.get("matKhau"));
        } catch (Exception e) {
            Log.e(TAG, "Error converting map to NguoiDung", e);
        }

        return nguoiDung;
    }

    // Convert ThongBao object to Map for Firebase
    public static Map<String, Object> convertThongBaoToMap(ThongBao thongBao) {
        Map<String, Object> thongBaoMap = new HashMap<>();
        thongBaoMap.put("maThongBao", thongBao.getMaThongBao());
        thongBaoMap.put("noiDung", thongBao.getNoiDung());
        thongBaoMap.put("trangThai", thongBao.getTrangThai());
        thongBaoMap.put("ngayThongBao", XDate.toStringDate(thongBao.getNgayThongBao()));
        // ✅ Lưu maNguoiDung (null = thông báo chung, có giá trị = thông báo cho user cụ thể)
        thongBaoMap.put("maNguoiDung", thongBao.getMaNguoiDung());
        // ✅ Lưu tieuDe
        thongBaoMap.put("tieuDe", thongBao.getTieuDe() != null ? thongBao.getTieuDe() : "Thông báo");
        thongBaoMap.put("lastModified", System.currentTimeMillis());
        return thongBaoMap;
    }

    // Convert HoaDonChiTiet object to Map for Firebase
    public static Map<String, Object> convertHoaDonChiTietToMap(HoaDonChiTiet hoaDonChiTiet) {
        Map<String, Object> hoaDonChiTietMap = new HashMap<>();
        hoaDonChiTietMap.put("maHDCT", hoaDonChiTiet.getMaHDCT());
        hoaDonChiTietMap.put("maHoaDon", hoaDonChiTiet.getMaHoaDon());
        hoaDonChiTietMap.put("maHangHoa", hoaDonChiTiet.getMaHangHoa());
        hoaDonChiTietMap.put("soLuong", hoaDonChiTiet.getSoLuong());
        hoaDonChiTietMap.put("giaTien", hoaDonChiTiet.getGiaTien());
        hoaDonChiTietMap.put("ghiChu", hoaDonChiTiet.getGhiChu());
        hoaDonChiTietMap.put("ngayXuatHoaDon", XDate.toStringDate(hoaDonChiTiet.getNgayXuatHoaDon()));
        hoaDonChiTietMap.put("lastModified", System.currentTimeMillis());
        return hoaDonChiTietMap;
    }

    // Convert HoaDonMangVe object to Map for Firebase
    public static Map<String, Object> convertHoaDonMangVeToMap(HoaDonMangVe hoaDonMangVe) {
        Map<String, Object> hoaDonMangVeMap = new HashMap<>();
        hoaDonMangVeMap.put("maHoaDon", hoaDonMangVe.getMaHoaDon());
        hoaDonMangVeMap.put("gioVao", XDate.toStringDateTime(hoaDonMangVe.getGioVao()));
        hoaDonMangVeMap.put("gioRa", XDate.toStringDateTime(hoaDonMangVe.getGioRa()));
        hoaDonMangVeMap.put("trangThai", hoaDonMangVe.getTrangThai());
        hoaDonMangVeMap.put("maKhachHang", hoaDonMangVe.getMaKhachHang());
        hoaDonMangVeMap.put("ghiChu", hoaDonMangVe.getGhiChu());
        hoaDonMangVeMap.put("lastModified", System.currentTimeMillis());
        return hoaDonMangVeMap;
    }

    // Convert HoaDon object to Map for Firebase
    public static Map<String, Object> convertHoaDonToMap(HoaDon hoaDon) {
        Map<String, Object> hoaDonMap = new HashMap<>();
        hoaDonMap.put("maHoaDon", hoaDon.getMaHoaDon());
        hoaDonMap.put("maBan", hoaDon.getMaBan());
        hoaDonMap.put("gioVao", XDate.toStringDateTime(hoaDon.getGioVao()));
        hoaDonMap.put("gioRa", XDate.toStringDateTime(hoaDon.getGioRa()));
        hoaDonMap.put("trangThai", hoaDon.getTrangThai());
        hoaDonMap.put("maKhachHang", hoaDon.getMaKhachHang());
        hoaDonMap.put("ghiChu", hoaDon.getGhiChu());
        hoaDonMap.put("lastModified", System.currentTimeMillis());
        return hoaDonMap;
    }

    // Convert Map to HoaDon object
    public static HoaDon convertMapToHoaDon(Map<String, Object> map) {
        HoaDon hoaDon = new HoaDon();
        try {
            hoaDon.setMaHoaDon(((Long) map.get("maHoaDon")).intValue());
            hoaDon.setMaBan(((Long) map.get("maBan")).intValue());

            try {
                hoaDon.setGioVao(XDate.toDateTime((String) map.get("gioVao")));
                hoaDon.setGioRa(XDate.toDateTime((String) map.get("gioRa")));
            } catch (ParseException e) {
                e.printStackTrace();
            }

            hoaDon.setTrangThai(((Long) map.get("trangThai")).intValue());
            hoaDon.setMaKhachHang((String) map.get("maKhachHang"));
            hoaDon.setGhiChu((String) map.get("ghiChu"));
        } catch (Exception e) {
            Log.e(TAG, "Error converting map to HoaDon", e);
        }

        return hoaDon;
    }

    // Convert Map to HoaDonChiTiet object
    public static HoaDonChiTiet convertMapToHoaDonChiTiet(Map<String, Object> map) {
        HoaDonChiTiet hoaDonChiTiet = new HoaDonChiTiet();
        try {
            hoaDonChiTiet.setMaHDCT(((Long) map.get("maHDCT")).intValue());
            hoaDonChiTiet.setMaHoaDon(((Long) map.get("maHoaDon")).intValue());
            hoaDonChiTiet.setMaHangHoa(((Long) map.get("maHangHoa")).intValue());
            hoaDonChiTiet.setSoLuong(((Long) map.get("soLuong")).intValue());
            hoaDonChiTiet.setGiaTien(((Long) map.get("giaTien")).intValue());
            hoaDonChiTiet.setGhiChu((String) map.get("ghiChu"));

            try {
                hoaDonChiTiet.setNgayXuatHoaDon(XDate.toDate((String) map.get("ngayXuatHoaDon")));
            } catch (ParseException e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error converting map to HoaDonChiTiet", e);
        }

        return hoaDonChiTiet;
    }

    // Convert Map to HoaDonMangVe object
    public static HoaDonMangVe convertMapToHoaDonMangVe(Map<String, Object> map) {
        HoaDonMangVe hoaDonMangVe = new HoaDonMangVe();
        try {
            hoaDonMangVe.setMaHoaDon(((Long) map.get("maHoaDon")).intValue());

            try {
                hoaDonMangVe.setGioVao(XDate.toDateTime((String) map.get("gioVao")));
                hoaDonMangVe.setGioRa(XDate.toDateTime((String) map.get("gioRa")));
            } catch (ParseException e) {
                e.printStackTrace();
            }

            hoaDonMangVe.setTrangThai(((Long) map.get("trangThai")).intValue());
            hoaDonMangVe.setMaKhachHang((String) map.get("maKhachHang"));
            hoaDonMangVe.setGhiChu((String) map.get("ghiChu"));
        } catch (Exception e) {
            Log.e(TAG, "Error converting map to HoaDonMangVe", e);
        }

        return hoaDonMangVe;
    }

    // Convert Map to ThongBao object
    public static ThongBao convertMapToThongBao(Map<String, Object> map) {
        ThongBao thongBao = new ThongBao();
        try {
            thongBao.setMaThongBao(((Long) map.get("maThongBao")).intValue());
            thongBao.setNoiDung((String) map.get("noiDung"));
            thongBao.setTrangThai(((Long) map.get("trangThai")).intValue());
            
            // Lấy maNguoiDung (có thể null - thông báo chung)
            Object maNguoiDungObj = map.get("maNguoiDung");
            if (maNguoiDungObj != null) {
                thongBao.setMaNguoiDung((String) maNguoiDungObj);
            }
            
            // Lấy tieuDe (có thể null)
            Object tieuDeObj = map.get("tieuDe");
            if (tieuDeObj != null) {
                thongBao.setTieuDe((String) tieuDeObj);
            } else {
                thongBao.setTieuDe("Thông báo");
            }

            try {
                thongBao.setNgayThongBao(XDate.toDate((String) map.get("ngayThongBao")));
            } catch (ParseException e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error converting map to ThongBao", e);
        }

        return thongBao;
    }

    // Convert DatBan object to Map for Firebase
    public static Map<String, Object> convertDatBanToMap(DatBan datBan) {
        Map<String, Object> datBanMap = new HashMap<>();
        datBanMap.put("maDatBan", datBan.getMaDatBan());
        datBanMap.put("maBan", datBan.getMaBan());
        datBanMap.put("maKhachHang", datBan.getMaKhachHang());
        datBanMap.put("ngayGioDat", datBan.getNgayGioDat());
        datBanMap.put("ngayGioSuDung", datBan.getNgayGioSuDung());
        datBanMap.put("trangThai", datBan.getTrangThai());
        datBanMap.put("ghiChu", datBan.getGhiChu() != null ? datBan.getGhiChu() : "");
        datBanMap.put("lastModified", datBan.getLastModified() > 0 ? datBan.getLastModified() : System.currentTimeMillis());
        return datBanMap;
    }

    // Convert Map to DatBan object
    public static DatBan convertMapToDatBan(Map<String, Object> map) {
        DatBan datBan = new DatBan();
        try {
            datBan.setMaDatBan(((Long) map.get("maDatBan")).intValue());
            datBan.setMaBan(((Long) map.get("maBan")).intValue());
            datBan.setMaKhachHang((String) map.get("maKhachHang"));
            datBan.setNgayGioDat((String) map.get("ngayGioDat"));
            datBan.setNgayGioSuDung((String) map.get("ngayGioSuDung"));
            datBan.setTrangThai(((Long) map.get("trangThai")).intValue());
            datBan.setGhiChu((String) map.get("ghiChu"));
            
            // Parse thông tin bổ sung (nếu có)
            if (map.get("tenBan") != null) {
                datBan.setTenBan((String) map.get("tenBan"));
            }
            if (map.get("tenKhachHang") != null) {
                datBan.setTenKhachHang((String) map.get("tenKhachHang"));
            }
            
            if (map.get("lastModified") != null) {
                datBan.setLastModified(((Long) map.get("lastModified")).longValue());
            } else {
                datBan.setLastModified(System.currentTimeMillis());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error converting map to DatBan", e);
        }
        return datBan;
    }
        
    /**
     * Interface for async map conversion callbacks
     */
    public interface OnMapConversionListener {
        void onSuccess(Map<String, Object> map);
        void onFailure(Exception e);
    }
}