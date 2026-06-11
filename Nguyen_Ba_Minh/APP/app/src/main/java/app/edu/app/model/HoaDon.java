package app.edu.app.model;

import androidx.annotation.NonNull;

import java.util.Date;

public class HoaDon implements java.io.Serializable{
    private int maHoaDon;
    private int maBan;
    private Date gioVao;
    private Date gioRa;
    private int trangThai;
    public static final int DA_THANH_TOAN = 1;
    public static final int CHUA_THANH_TOAN = 0;
    public static final int DA_DUYET = 2;
    public static final int CHUA_DUYET = 3;
    private String maKhachHang;
    private String ghiChu;
    public HoaDon() {
    }

    public HoaDon(int maBan, Date gioVao, Date gioRa, int trangThai) {
        this.maBan = maBan;
        this.gioVao = gioVao;
        this.gioRa = gioRa;
        this.trangThai = trangThai;
    }

    public HoaDon(int maHoaDon, int maBan, Date gioVao, Date gioRa, int trangThai) {
        this.maHoaDon = maHoaDon;
        this.maBan = maBan;
        this.gioVao = gioVao;
        this.gioRa = gioRa;
        this.trangThai = trangThai;
    }

    public HoaDon(int maHoaDon, Date gioVao, Date gioRa, int trangThai, String maKhachHang,int maBan) {
        this.maHoaDon = maHoaDon;
        this.gioVao = gioVao;
        this.gioRa = gioRa;
        this.trangThai = trangThai;
        this.maKhachHang = maKhachHang;
        this.maBan = maBan;
    }

    public int getMaHoaDon() {
        return maHoaDon;
    }

    public void setMaHoaDon(int maHoaDon) {
        this.maHoaDon = maHoaDon;
    }

    public int getMaBan() {
        return maBan;
    }

    public void setMaBan(int maBan) {
        this.maBan = maBan;
    }


    public Date getGioVao() {
        return gioVao;
    }

    public void setGioVao(Date gioVao) {
        this.gioVao = gioVao;
    }

    public Date getGioRa() {
        return gioRa;
    }

    public void setGioRa(Date gioRa) {
        this.gioRa = gioRa;
    }

    public int getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(int trangThai) {
        this.trangThai = trangThai;
    }

    public String getMaKhachHang() {
        return maKhachHang;
    }

    public void setMaKhachHang(String maKhachHang) {
        this.maKhachHang = maKhachHang;
    }

    public String getGhiChu() {
        return ghiChu;
    }

    public void setGhiChu(String ghiChu) {
        this.ghiChu = ghiChu;
    }

    @NonNull
    @Override
    public String toString() {
        return "HoaDon{" +
                "maHoaDon=" + maHoaDon +
                ", maBan=" + maBan +
                ", gioVao=" + gioVao +
                ", gioRa=" + gioRa +
                ", trangThai=" + trangThai +
                '}';
    }
}
