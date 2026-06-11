package app.edu.app.model;

import androidx.annotation.NonNull;

import java.util.Arrays;

public class HangHoa {
    private int maHangHoa;
    private String tenHangHoa;
    private byte[] hinhAnh;
    private int giaTien;
    private int maLoai;
    private int trangThai;
    public static final int STATUS_STILL = 1; // Còn hàng
    public static final int STATUS_OVER = 0; // hết hàng

    public HangHoa() {
    }

    public HangHoa(String tenHangHoa, byte[] hinhAnh, int giaTien, int maLoai, int trangThai) {
        this.tenHangHoa = tenHangHoa;
        this.hinhAnh = hinhAnh;
        this.giaTien = giaTien;
        this.maLoai = maLoai;
        this.trangThai = trangThai;
    }

    public HangHoa(int maHangHoa, String tenHangHoa, byte[] hinhAnh, int giaTien, int maLoai, int trangThai) {
        this.maHangHoa = maHangHoa;
        this.tenHangHoa = tenHangHoa;
        this.hinhAnh = hinhAnh;
        this.giaTien = giaTien;
        this.maLoai = maLoai;
        this.trangThai = trangThai;
    }

    public int getMaHangHoa() {
        return maHangHoa;
    }

    public void setMaHangHoa(int maHangHoa) {
        this.maHangHoa = maHangHoa;
    }

    public String getTenHangHoa() {
        return tenHangHoa;
    }

    public void setTenHangHoa(String tenHangHoa) {
        this.tenHangHoa = tenHangHoa;
    }

    public byte[] getHinhAnh() {
        return hinhAnh;
    }

    public void setHinhAnh(byte[] hinhAnh) {
        this.hinhAnh = hinhAnh;
    }

    public int getGiaTien() {
        return giaTien;
    }

    public void setGiaTien(int giaTien) {
        this.giaTien = giaTien;
    }

    public int getMaLoai() {
        return maLoai;
    }

    public void setMaLoai(int maLoai) {
        this.maLoai = maLoai;
    }

    public int getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(int trangThai) {
        this.trangThai = trangThai;
    }

    @NonNull
    @Override
    public String toString() {
        return "HangHoa{" +
                "maHangHoa=" + maHangHoa +
                ", tenHangHoa='" + tenHangHoa + '\'' +
                ", hinhAnh=" + Arrays.toString(hinhAnh) +
                ", giaTien=" + giaTien +
                ", maLoai=" + maLoai +
                ", trangThai=" + trangThai +
                '}';
    }
}
