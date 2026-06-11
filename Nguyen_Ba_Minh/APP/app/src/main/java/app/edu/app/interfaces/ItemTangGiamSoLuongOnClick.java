package app.edu.app.interfaces;

import android.view.View;

import app.edu.app.model.HangHoa;
import app.edu.app.model.HoaDonChiTiet;

public interface ItemTangGiamSoLuongOnClick {
    void itemOclick(View view, int indext, HoaDonChiTiet hoaDonChiTiet, HangHoa hangHoa);
    void itemOclickDeleteHDCT(View view, HoaDonChiTiet hoaDonChiTiet);
}
