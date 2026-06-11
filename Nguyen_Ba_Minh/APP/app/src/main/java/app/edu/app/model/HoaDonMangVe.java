package app.edu.app.model;

import androidx.annotation.NonNull;

import java.util.Date;

public class HoaDonMangVe implements java.io.Serializable{

    private int maHoaDon;
    private Date gioVao;
    private Date gioRa;
    private int trangThai;
    private String maKhachHang;
    private String ghiChu;
    public static final int DA_DUYET = 0;
    public static final int CHUA_DUYET = 1;
    public static final int HUY_HOA_DON = 2;
    public static final int CHUA_XAC_NHAN =-1;
    public HoaDonMangVe() {
    }

    public HoaDonMangVe( Date gioVao, Date gioRa, int trangThai) {
        this.gioVao = gioVao;
        this.gioRa = gioRa;
        this.trangThai = trangThai;
    }

    public HoaDonMangVe(int maHoaDon, int maBan, Date gioVao, Date gioRa, int trangThai) {
        this.maHoaDon = maHoaDon;
        this.gioVao = gioVao;
        this.gioRa = gioRa;
        this.trangThai = trangThai;
    }

    public HoaDonMangVe(int maHoaDon, Date gioVao, Date gioRa, int trangThai, String maKhachHang) {
        this.maHoaDon = maHoaDon;
        this.gioVao = gioVao;
        this.gioRa = gioRa;
        this.trangThai = trangThai;
        this.maKhachHang = maKhachHang;
    }

    public String getMaKhachHang() {
        return maKhachHang;
    }

    public void setMaKhachHang(String maKhachHang) {
        this.maKhachHang = maKhachHang;
    }

    public int getMaHoaDon() {
        return maHoaDon;
    }

    public void setMaHoaDon(int maHoaDon) {
        this.maHoaDon = maHoaDon;
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
                ", gioVao=" + gioVao +
                ", gioRa=" + gioRa +
                ", trangThai=" + trangThai +
                '}';
    }
}
