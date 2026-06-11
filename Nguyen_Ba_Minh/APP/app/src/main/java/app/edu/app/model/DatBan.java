package app.edu.app.model;

import androidx.annotation.NonNull;

import java.util.Date;

/**
 * Model cho đặt bàn (Table Reservation/Booking)
 * Khách hàng đặt bàn, admin/nhân viên duyệt
 */
public class DatBan {
    private int maDatBan;
    private int maBan;              // Bàn được đặt
    private String maKhachHang;     // Khách hàng đặt bàn
    private String ngayGioDat;      // Ngày giờ đặt (format: "dd-MM-yyyy HH:mm:ss")
    private String ngayGioSuDung;   // Ngày giờ sử dụng bàn (format: "dd-MM-yyyy HH:mm:ss")
    private int trangThai;          // Trạng thái đặt bàn
    private String ghiChu;          // Ghi chú
    private long lastModified;      // Thời gian chỉnh sửa cuối
    
    // Thông tin bổ sung (dùng để hiển thị, sync từ Firebase)
    private String tenBan;          // Tên bàn (VD: "Bàn 8")
    private String tenKhachHang;    // Tên khách hàng (VD: "Nguyễn Anh")
    
    // Trạng thái đặt bàn
    public static final int TRANG_THAI_CHO_DUYET = 0;    // Chờ duyệt
    public static final int TRANG_THAI_DA_DUYET = 1;     // Đã duyệt
    public static final int TRANG_THAI_TU_CHOI = -1;     // Từ chối
    public static final int TRANG_THAI_DA_SU_DUNG = 2;   // Đã sử dụng
    public static final int TRANG_THAI_DA_HUY = 3;       // Đã hủy

    public DatBan() {
    }

    public DatBan(int maDatBan, int maBan, String maKhachHang, String ngayGioDat, 
                  String ngayGioSuDung, int trangThai, String ghiChu) {
        this.maDatBan = maDatBan;
        this.maBan = maBan;
        this.maKhachHang = maKhachHang;
        this.ngayGioDat = ngayGioDat;
        this.ngayGioSuDung = ngayGioSuDung;
        this.trangThai = trangThai;
        this.ghiChu = ghiChu;
        this.lastModified = System.currentTimeMillis();
    }

    public int getMaDatBan() {
        return maDatBan;
    }

    public void setMaDatBan(int maDatBan) {
        this.maDatBan = maDatBan;
    }

    public int getMaBan() {
        return maBan;
    }

    public void setMaBan(int maBan) {
        this.maBan = maBan;
    }

    public String getMaKhachHang() {
        return maKhachHang;
    }

    public void setMaKhachHang(String maKhachHang) {
        this.maKhachHang = maKhachHang;
    }

    public String getNgayGioDat() {
        return ngayGioDat;
    }

    public void setNgayGioDat(String ngayGioDat) {
        this.ngayGioDat = ngayGioDat;
    }

    public String getNgayGioSuDung() {
        return ngayGioSuDung;
    }

    public void setNgayGioSuDung(String ngayGioSuDung) {
        this.ngayGioSuDung = ngayGioSuDung;
    }

    public int getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(int trangThai) {
        this.trangThai = trangThai;
    }

    public String getGhiChu() {
        return ghiChu;
    }

    public void setGhiChu(String ghiChu) {
        this.ghiChu = ghiChu;
    }

    public long getLastModified() {
        return lastModified;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    public String getTenBan() {
        return tenBan;
    }

    public void setTenBan(String tenBan) {
        this.tenBan = tenBan;
    }

    public String getTenKhachHang() {
        return tenKhachHang;
    }

    public void setTenKhachHang(String tenKhachHang) {
        this.tenKhachHang = tenKhachHang;
    }

    /**
     * Kiểm tra đặt bàn có đang chờ duyệt không
     */
    public boolean isPending() {
        return trangThai == TRANG_THAI_CHO_DUYET;
    }

    /**
     * Kiểm tra đặt bàn đã được duyệt chưa
     */
    public boolean isApproved() {
        return trangThai == TRANG_THAI_DA_DUYET;
    }

    /**
     * Kiểm tra đặt bàn đã bị từ chối chưa
     */
    public boolean isRejected() {
        return trangThai == TRANG_THAI_TU_CHOI;
    }

    @NonNull
    @Override
    public String toString() {
        return "DatBan{" +
                "maDatBan=" + maDatBan +
                ", maBan=" + maBan +
                ", maKhachHang='" + maKhachHang + '\'' +
                ", ngayGioDat='" + ngayGioDat + '\'' +
                ", ngayGioSuDung='" + ngayGioSuDung + '\'' +
                ", trangThai=" + trangThai +
                ", ghiChu='" + ghiChu + '\'' +
                '}';
    }
}

