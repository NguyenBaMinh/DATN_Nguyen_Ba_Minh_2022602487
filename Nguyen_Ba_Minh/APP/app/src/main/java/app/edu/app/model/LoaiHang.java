package app.edu.app.model;

import androidx.annotation.NonNull;

import java.util.Arrays;

public class LoaiHang {
    private int maLoai;
    private byte[] hinhAnh;
    private String tenLoai; 

    public LoaiHang() {
        this.hinhAnh = hinhAnh;
        this.tenLoai = tenLoai;
    }

    public LoaiHang(int maLoai, byte[] hinhAnh, String tenLoai) {
        this.maLoai = maLoai;
        this.hinhAnh = hinhAnh;
        this.tenLoai = tenLoai;
    }

    public int getMaLoai() {
        return maLoai;
    }

    public void setMaLoai(int maLoai) {
        this.maLoai = maLoai;
    }

    public byte[] getHinhAnh() {
        return hinhAnh;
    }

    public void setHinhAnh(byte[] hinhAnh) {
        this.hinhAnh = hinhAnh;
    }

    public String getTenLoai() {
        return tenLoai;
    }

    public void setTenLoai(String tenLoai) {
        this.tenLoai = tenLoai;
    }

    @NonNull
    @Override
    public String toString() {
        return "LoaiHang{" +
                "maLoai=" + maLoai +
                ", hinhAnh=" + Arrays.toString(hinhAnh) +
                ", tenLoai='" + tenLoai + '\'' +
                '}';
    }
}

