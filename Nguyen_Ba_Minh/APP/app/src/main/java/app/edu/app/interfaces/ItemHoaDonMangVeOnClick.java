package app.edu.app.interfaces;

import android.view.View;

import app.edu.app.model.HoaDonMangVe;

public interface ItemHoaDonMangVeOnClick {
    void itemOclick(View view, HoaDonMangVe hoaDonMangVe);

    void itemDuyet(View view, HoaDonMangVe hoaDonMangVe);

    void itemHuy(View view, HoaDonMangVe hoaDonMangVe);
}
