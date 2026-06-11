package app.edu.app.model;

import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.Date;

public class NguoiDung {
    public static final String POSITION_CUSTOMER = "User";
    private String maNguoiDung;
    private String hoVaTen;
    private byte[] hinhAnh;
    private Date ngaySinh;
    private String email;
    private String chucVu;
    private String gioiTinh;
    private String matKhau;
    public static final String GENDER_MALE = "Nam";
    public static final String GENDER_FEMALE = "Nu";
    public static final String POSITION_ADMIN = "Admin";
    public static final String POSITION_STAFF = "NhanVien";
    public static final String MATCHES_EMAIL = "^[\\w-.]+@([\\w-]+\\.)+[\\w-]{2,4}$";

    public NguoiDung(String maNguoiDung, String hoVaTen, String email, String chucVu, String gioiTinh, String matKhau) {
        this.maNguoiDung = maNguoiDung;
        this.hoVaTen = hoVaTen;
        this.email = email;
        this.chucVu = chucVu;
        this.gioiTinh = gioiTinh;
        this.matKhau = matKhau;
    }

    public NguoiDung() {
    }

    public NguoiDung(String maNguoiDung, String hoVaTen, byte[] hinhAnh, Date ngaySinh, String email, String chucVu, String gioiTinh, String matKhau) {
        this.maNguoiDung = maNguoiDung;
        this.hoVaTen = hoVaTen;
        this.hinhAnh = hinhAnh;
        this.ngaySinh = ngaySinh;
        this.email = email;
        this.chucVu = chucVu;
        this.gioiTinh = gioiTinh;
        this.matKhau = matKhau;
    }

    public String getMaNguoiDung() {
        return maNguoiDung;
    }

    public void setMaNguoiDung(String maNguoiDung) {
        this.maNguoiDung = maNguoiDung;
    }

    public String getHoVaTen() {
        return hoVaTen;
    }

    public void setHoVaTen(String hoVaTen) {
        this.hoVaTen = hoVaTen;
    }

    public byte[] getHinhAnh() {
        return hinhAnh;
    }

    public void setHinhAnh(byte[] hinhAnh) {
        this.hinhAnh = hinhAnh;
    }

    public Date getNgaySinh() {
        return ngaySinh;
    }

    public void setNgaySinh(Date ngaySinh) {
        this.ngaySinh = ngaySinh;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getChucVu() {
        return chucVu;
    }

    public void setChucVu(String chucVu) {
        this.chucVu = chucVu;
    }

    public String getGioiTinh() {
        return gioiTinh;
    }

    public void setGioiTinh(String gioiTinh) {
        this.gioiTinh = gioiTinh;
    }

    public String getMatKhau() {
        return matKhau;
    }

    public void setMatKhau(String matKhau) {
        this.matKhau = matKhau;
    }

    public boolean isAdmin(){
        return this.chucVu.equals(POSITION_ADMIN);
    }

    public boolean isStaff(){
        return this.chucVu.equals(POSITION_STAFF);
    }

    @NonNull
    @Override
    public String toString() {
        return "NguoiDung{" +
                "maNguoiDung='" + maNguoiDung + '\'' +
                ", hoVaTen='" + hoVaTen + '\'' +
                ", hinhAnh=" + Arrays.toString(hinhAnh) +
                ", ngaySinh=" + ngaySinh +
                ", email='" + email + '\'' +
                ", chucVu='" + chucVu + '\'' +
                ", gioiTinh='" + gioiTinh + '\'' +
                ", matKhau='" + matKhau + '\'' +
                '}';
    }
}
