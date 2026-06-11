package app.edu.app.model;

import androidx.annotation.NonNull;

import java.util.Date;

public class ThongBao {
    private int maThongBao;
    private int trangThai;
    private String noiDung;
    private Date ngayThongBao;
    private String maNguoiDung; // Người nhận thông báo (null = thông báo chung cho tất cả)
    private String tieuDe; // Tiêu đề thông báo
    
    public static final int STATUS_DA_XEM = 1;  // trạng thái đã xem
    public static final int STATUS_CHUA_XEM = 0; // trạng thái chưa xem
    
    public ThongBao() {
    }

    public ThongBao(int maThongBao, int trangThai, String noiDung, Date ngayThongBao) {
        this.maThongBao = maThongBao;
        this.trangThai = trangThai;
        this.noiDung = noiDung;
        this.ngayThongBao = ngayThongBao;
        this.maNguoiDung = null; // Default: thông báo chung
        this.tieuDe = "Thông báo"; // Default title
    }
    
    public ThongBao(int maThongBao, int trangThai, String noiDung, Date ngayThongBao, String maNguoiDung, String tieuDe) {
        this.maThongBao = maThongBao;
        this.trangThai = trangThai;
        this.noiDung = noiDung;
        this.ngayThongBao = ngayThongBao;
        this.maNguoiDung = maNguoiDung;
        this.tieuDe = tieuDe;
    }

    public int getMaThongBao() {
        return maThongBao;
    }

    public void setMaThongBao(int maThongBao) {
        this.maThongBao = maThongBao;
    }

    public int getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(int trangThai) {
        this.trangThai = trangThai;
    }

    public String getNoiDung() {
        return noiDung;
    }

    public void setNoiDung(String noiDung) {
        this.noiDung = noiDung;
    }

    public Date getNgayThongBao() {
        return ngayThongBao;
    }

    public void setNgayThongBao(Date ngayThongBao) {
        this.ngayThongBao = ngayThongBao;
    }
    
    public String getMaNguoiDung() {
        return maNguoiDung;
    }
    
    public void setMaNguoiDung(String maNguoiDung) {
        this.maNguoiDung = maNguoiDung;
    }
    
    public String getTieuDe() {
        return tieuDe;
    }
    
    public void setTieuDe(String tieuDe) {
        this.tieuDe = tieuDe;
    }

    @NonNull
    @Override
    public String toString() {
        return "ThongBao{" +
                "maThongBao=" + maThongBao +
                ", trangThai=" + trangThai +
                ", noiDung='" + noiDung + '\'' +
                ", ngayThongBao=" + ngayThongBao +
                ", maNguoiDung='" + maNguoiDung + '\'' +
                ", tieuDe='" + tieuDe + '\'' +
                '}';
    }
}
