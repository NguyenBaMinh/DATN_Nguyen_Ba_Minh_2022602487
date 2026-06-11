package app.edu.app.fragment;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Objects;

import app.edu.app.ui.DatBanActivity;
import de.hdodenhof.circleimageview.CircleImageView;
import app.edu.app.MainActivity;
import app.edu.app.R;
import app.edu.app.adapter.DatBanAdapter;
import app.edu.app.adapter.PhotoAdapter;
import app.edu.app.adapter.ThucUongHomeFragmentAdapter;
import app.edu.app.dao.DatBanDAO;
import app.edu.app.dao.HangHoaDAO;
import app.edu.app.dao.HoaDonChiTietDAO;
import app.edu.app.dao.HoaDonDAO;
import app.edu.app.dao.HoaDonMangVeDao;
import app.edu.app.dao.NguoiDungDAO;
import app.edu.app.interfaces.ItemDatBanOnClick;
import app.edu.app.interfaces.OnUrlFetchedListener;
import app.edu.app.model.DatBan;
import app.edu.app.model.HangHoa;
import app.edu.app.model.HoaDon;
import app.edu.app.model.HoaDonChiTiet;
import app.edu.app.model.HoaDonMangVe;
import app.edu.app.model.NguoiDung;
import app.edu.app.model.Photo;
import app.edu.app.ui.DoanhThuActivity;
import app.edu.app.ui.DuyetActivity;
import app.edu.app.ui.HoaDonActivity;
import app.edu.app.ui.HoaDonNguoiDungActivity;
import app.edu.app.ui.LoaiThucUongActivity;
import app.edu.app.ui.MangVeActivity;
import app.edu.app.ui.NhanVienActivity;
import app.edu.app.ui.OderActivity;
import app.edu.app.ui.QuanLyBanActivity;
import app.edu.app.ui.QuanLyBanNguoiDungActivity;
import app.edu.app.ui.ThucUongActivity;
import app.edu.app.utils.ImageCache;
import app.edu.app.utils.MyToast;
import app.edu.app.utils.XDate;
import me.relex.circleindicator.CircleIndicator3;

public class HomeFragment extends Fragment implements View.OnClickListener {
    TextView tvHi;
    CircleImageView civHinhAnh;
    ViewPager2 vpSlideImage;
    CircleIndicator3 indicator3;
    CardView cvBan, cvLoai, cvThucUong, cvNhanVien, cvHoaDon, cvDoanhThu, cvDuyet, cvMangVe, cvHoaDon2, cvDatBan, cvQuanLyBanNguoiDung;
    MainActivity mainActivity;
    NguoiDungDAO nguoiDungDAO;
    HangHoaDAO hangHoaDAO;
    DatBanDAO datBanDAO;
    HoaDonDAO hoaDonDAO;
    HoaDonChiTietDAO hoaDonChiTietDAO;
    RecyclerView recyclerViewThucUong;
    Handler handler;
    Runnable runnable;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Initialize DAOs first to check user role
        mainActivity = ((MainActivity) getActivity());
        nguoiDungDAO = new NguoiDungDAO(getContext());
        hangHoaDAO = new HangHoaDAO(getContext());
        datBanDAO = new DatBanDAO(getContext());
        hoaDonDAO = new HoaDonDAO(getContext());
        hoaDonChiTietDAO = new HoaDonChiTietDAO(getContext());
        
        View view;
        
        // Kiểm tra xem user đã đăng nhập chưa
        if (mainActivity != null && !mainActivity.isUserLoggedIn()) {
            // Chưa đăng nhập - hiển thị layout user (guest mode)
            view = inflater.inflate(R.layout.fragment_home_user, container, false);
        } else {
            // Đã đăng nhập - Get user để quyết định layout
            NguoiDung currentUser = getNguoiDung();
            
            // Inflate appropriate layout based on user role
            if (currentUser != null && (currentUser.getChucVu().equals("Admin") || currentUser.getChucVu().equals("NhanVien"))) {
                // Admin/Staff layout (4x2 grid with 8 management cards)
                view = inflater.inflate(R.layout.fragment_home_admin, container, false);
            } else {
                // User layout (2x3 grid with 6 customer cards)
                view = inflater.inflate(R.layout.fragment_home_user, container, false);
            }
        }
        
        initView(view);
        initOnClickCard();
        loadSlideImage();

        welcomeUser();
        loadListThucUong();
        autoRunSildeImage();
        return view;
    }

    private void autoRunSildeImage() {
        // Tự động chuyển ảnh trong SlideImage
        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                if (vpSlideImage.getCurrentItem() == getListImage().size() - 1) {
                    vpSlideImage.setCurrentItem(0, false);
                } else {
                    vpSlideImage.setCurrentItem(vpSlideImage.getCurrentItem() + 1);
                }
            }
        };
        handler.postDelayed(runnable, 2000);

        // Sự kiện Slide Image chuyển ảnh
        vpSlideImage.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                handler.removeCallbacks(runnable);
                handler.postDelayed(runnable, 2000);
            }
        });
    }

    private void loadListThucUong() {
        if (recyclerViewThucUong == null) {
            return; // RecyclerView không tồn tại trong layout user
        }
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext(), RecyclerView.HORIZONTAL, false);
        recyclerViewThucUong.setLayoutManager(linearLayoutManager);

        // Lấy danh sách thức uống hiển thị trên recyclerView
        ArrayList<HangHoa> listHangHoa = hangHoaDAO.getAll();
        ThucUongHomeFragmentAdapter adapter = new ThucUongHomeFragmentAdapter(listHangHoa, new app.edu.app.interfaces.ItemHangHoaOnClick() {
            @Override
            public void itemOclick(View view, HangHoa hangHoa) {
                // Kiểm tra đăng nhập trước khi cho phép chọn
                if (mainActivity != null && !mainActivity.isUserLoggedIn()) {
                    MyToast.error(getContext(), "Vui lòng đăng nhập để đặt hàng");
                    mainActivity.requireLogin();
                    return;
                }
                // Hiển thị dialog chọn loại đặt hàng
                showChonLoaiDatHangDialog(hangHoa);
            }
        });
        recyclerViewThucUong.setAdapter(adapter);
    }

    private void initView(View view) {
        vpSlideImage = view.findViewById(R.id.vpSlideImage);
        indicator3 = view.findViewById(R.id.circleIndicator3);
        cvBan = view.findViewById(R.id.cardBan);
        cvLoai = view.findViewById(R.id.cardLoaiThucUong);
        cvThucUong = view.findViewById(R.id.cardThucUong);
        cvNhanVien = view.findViewById(R.id.cardNhanVien);
        cvHoaDon = view.findViewById(R.id.cardHoaDon);
        cvDoanhThu = view.findViewById(R.id.cardDoanhThu);
        cvDuyet = view.findViewById(R.id.cardDuyet);
        cvMangVe = view.findViewById(R.id.cardMangVe);
        cvHoaDon2 = view.findViewById(R.id.cardHoaDon2);
        cvDatBan = view.findViewById(R.id.cardDatBan);
        cvQuanLyBanNguoiDung = view.findViewById(R.id.cardQuanLyBanNguoiDung);
        tvHi = view.findViewById(R.id.tvHi);
        civHinhAnh = view.findViewById(R.id.hinhAnh);
        recyclerViewThucUong = view.findViewById(R.id.recyclerViewThucUong);
    }

    private void initOnClickCard() {
        if (cvBan != null) cvBan.setOnClickListener(this);
        if (cvLoai != null) cvLoai.setOnClickListener(this);
        if (cvThucUong != null) cvThucUong.setOnClickListener(this);
        if (cvNhanVien != null) cvNhanVien.setOnClickListener(this);
        if (cvHoaDon != null) cvHoaDon.setOnClickListener(this);
        if (cvDoanhThu != null) cvDoanhThu.setOnClickListener(this);
        if (cvDuyet != null) cvDuyet.setOnClickListener(this);
        if (cvMangVe != null) cvMangVe.setOnClickListener(this);
        if (cvHoaDon2 != null) cvHoaDon2.setOnClickListener(this);
        if (cvDatBan != null) cvDatBan.setOnClickListener(this);
        if (cvQuanLyBanNguoiDung != null) cvQuanLyBanNguoiDung.setOnClickListener(this);
    }

    private void loadSlideImage() {
        // Hiển thị Slide image
        PhotoAdapter adapter = new PhotoAdapter(getListImage());

        vpSlideImage.setAdapter(adapter);
        vpSlideImage.setOffscreenPageLimit(2);
        indicator3.setViewPager(vpSlideImage);
    }

    @NonNull
    private ArrayList<Photo> getListImage() {
        ArrayList<Photo> list = new ArrayList<>();
        list.add(new Photo(R.drawable.slide_image1));
        list.add(new Photo(R.drawable.slide_image2));
        list.add(new Photo(R.drawable.slide_image3));
        list.add(new Photo(R.drawable.silde_image4));
        list.add(new Photo(R.drawable.slide_image5));

        return list;
    }

    @SuppressLint("SetTextI18n")
    private void welcomeUser() {
        // Kiểm tra xem user đã đăng nhập chưa
        if (mainActivity != null && !mainActivity.isUserLoggedIn()) {
            // Chưa đăng nhập - hiển thị thông tin mặc định
            tvHi.setText("Xin chào!");
            civHinhAnh.setImageResource(R.drawable.avatar_user);
            return;
        }
        
        NguoiDung nguoiDung = getNguoiDung();
        if (nguoiDung == null) {
            // Không lấy được thông tin user
            tvHi.setText("Xin chào!");
            civHinhAnh.setImageResource(R.drawable.avatar_user);
            return;
        }
        
        ImageCache.getUrlFromCache("nguoidung_" + nguoiDung.getMaNguoiDung(), new OnUrlFetchedListener() {
            @Override
            public void onUrlFetched(String url) {
                Picasso.get().load(url).into(civHinhAnh);
            }
        });

        // Gán dữ liệu cho view
        tvHi.setText("Hello, " + nguoiDung.getHoVaTen());
        
        // Không cần logic ẩn/hiện nữa vì đã tách thành 2 layout riêng
        // Layout được chọn từ onCreateView dựa trên user role
    }

    private NguoiDung getNguoiDung() {
        // Lấy mã người dùng từ MainActivity thông qua hàm getKeyUser
        String maNguoiDung = Objects.requireNonNull(mainActivity).getKeyUser();
        // Lây đối tượng người dùng theo mã
        return nguoiDungDAO.getByMaNguoiDung(maNguoiDung);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View view) {
        // Kiểm tra đã đăng nhập chưa trước khi cho phép thao tác
        if (mainActivity != null && !mainActivity.isUserLoggedIn()) {
            MyToast.error(getContext(), "Vui lòng đăng nhập để sử dụng chức năng này");
            mainActivity.requireLogin();
            return;
        }
        
//        switch (view.getId()) {
//            case R.id.cardBan:
//            case R.id.cardBan2:
//                // Mở màng hình quản lý bàn
//                startActivity(new Intent(getContext(), QuanLyBanActivity.class));
//                (requireActivity()).overridePendingTransition(R.anim.anim_in_right, R.anim.anim_out_left);
//                break;
//            case R.id.cardLoaiThucUong:
//                // Mở màng hình quản lý loại hàng
//                startActivity(new Intent(getContext(), LoaiThucUongActivity.class));
//                (requireActivity()).overridePendingTransition(R.anim.anim_in_right, R.anim.anim_out_left);
//                break;
//            case R.id.cardThucUong:
//                // Mở màng hình quản lý thức uống
//                startActivity(new Intent(getContext(), ThucUongActivity.class));
//                (requireActivity()).overridePendingTransition(R.anim.anim_in_right, R.anim.anim_out_left);
//                break;
//            case R.id.cardNhanVien:
//                if (getNguoiDung().isAdmin()) {
//                    // Người dùng có chức vụ ="Admin" -> Mở màng hình quản lý nhân viên
//                    startActivity(new Intent(getContext(), NhanVienActivity.class));
//                    (requireActivity()).overridePendingTransition(R.anim.anim_in_right, R.anim.anim_out_left);
//                } else {
//                    // Người dung có chức vụ = "NhanVien"
//                    MyToast.error(getContext(), "Chức năng dành cho Admin");
//                }
//                break;
//            case R.id.cardHoaDon:
//                // Mở màng hình quản lý hoá đơn
//                startActivity(new Intent(getContext(), HoaDonActivity.class));
//                (requireActivity()).overridePendingTransition(R.anim.anim_in_right, R.anim.anim_out_left);
//                break;
//            case R.id.cardHoaDon2:
//                // Mở màng hình quản lý hoá đơn
//                Intent intent1 = new Intent(getContext(), HoaDonNguoiDungActivity.class);
//                intent1.putExtra("maNguoiDung", getNguoiDung().getMaNguoiDung());
//                startActivity(intent1);
//                (requireActivity()).overridePendingTransition(R.anim.anim_in_right, R.anim.anim_out_left);
//                break;
//            case R.id.cardDuyet:
//                // Mở màng hình quản lý hoá đơn
//                startActivity(new Intent(getContext(), DuyetActivity.class));
//                (requireActivity()).overridePendingTransition(R.anim.anim_in_right, R.anim.anim_out_left);
//                break;
//            case R.id.cardMangVe:
//                Intent intent = new Intent(getContext(), MangVeActivity.class);
//                intent.putExtra("maNguoiDung", getNguoiDung().getMaNguoiDung());
//                Calendar c = Calendar.getInstance(); // lấy ngày thánh năm và giờ hiện tại
//                HoaDonMangVeDao hoaDonMangVeDao = new HoaDonMangVeDao(getContext());
//                if (!hoaDonMangVeDao.checkHoaDonChuaDuyet(getNguoiDung().getMaNguoiDung()) || !hoaDonMangVeDao.checkHoaDonChuaXacNhan(getNguoiDung().getMaNguoiDung())) {
//                    HoaDonMangVe hoaDonMangVe = new HoaDonMangVe();
//                    hoaDonMangVe.setMaKhachHang(getNguoiDung().getMaNguoiDung());
//                    hoaDonMangVe.setGioVao(c.getTime());
//                    hoaDonMangVe.setGioRa(c.getTime());
//                    hoaDonMangVe.setTrangThai(HoaDonMangVe.CHUA_XAC_NHAN);
//                    hoaDonMangVe.setGhiChu("");
//                    hoaDonMangVeDao.insertHoaDonMangVe(hoaDonMangVe);
//                }
//                startActivity(intent);
//                (requireActivity()).overridePendingTransition(R.anim.anim_in_right, R.anim.anim_out_left);
//                break;
//            case R.id.cardDoanhThu:
//                if (getNguoiDung().isAdmin()) {
//                    // Người dùng có chức vụ ="Admin" -> Mở màng hình quản lý doanh thu
//                    startActivity(new Intent(getContext(), DoanhThuActivity.class));
//                    (requireActivity()).overridePendingTransition(R.anim.anim_in_right, R.anim.anim_out_left);
//                } else {
//                    // Người dùng có chức vụ = "NhanVien"
//                    MyToast.error(getContext(), "Chức năng dành cho Admin");
//                }
//                break;
//            default:
//                break;
//        }

        if (view.getId() == R.id.cardBan) {
            // Mở màng hình quản lý bàn (chỉ cho Admin/NhanVien)
            if (getNguoiDung().getChucVu().equals("Admin") || getNguoiDung().getChucVu().equals("NhanVien")) {
                startActivity(new Intent(getContext(), QuanLyBanActivity.class));
                (requireActivity()).overridePendingTransition(R.anim.anim_in_right, R.anim.anim_out_left);
            } else {
                MyToast.error(getContext(), "Chức năng dành cho Admin/Nhân viên");
            }
        } else if (view.getId() == R.id.cardLoaiThucUong) {
            // Mở màng hình quản lý loại hàng
            startActivity(new Intent(getContext(), LoaiThucUongActivity.class));
            (requireActivity()).overridePendingTransition(R.anim.anim_in_right, R.anim.anim_out_left);
        } else if (view.getId() == R.id.cardThucUong) {
            // Mở màng hình quản lý thức uống
            startActivity(new Intent(getContext(), ThucUongActivity.class));
            (requireActivity()).overridePendingTransition(R.anim.anim_in_right, R.anim.anim_out_left);
        } else if (view.getId() == R.id.cardNhanVien) {
            if (getNguoiDung().isAdmin()) {
                // Người dùng có chức vụ ="Admin" -> Mở màng hình quản lý nhân viên
                startActivity(new Intent(getContext(), NhanVienActivity.class));
                (requireActivity()).overridePendingTransition(R.anim.anim_in_right, R.anim.anim_out_left);
            } else {
                // Người dung có chức vụ = "NhanVien"
                MyToast.error(getContext(), "Chức năng dành cho Admin");
            }
        } else if (view.getId() == R.id.cardHoaDon2) {
            // Mở màng hình quản lý hoá đơn
            startActivity(new Intent(getContext(), HoaDonActivity.class));
            (requireActivity()).overridePendingTransition(R.anim.anim_in_right, R.anim.anim_out_left);
        } else if (view.getId() == R.id.cardHoaDon) {
            // Mở màng hình quản lý hoá đơn
            Intent intent1 = new Intent(getContext(), HoaDonNguoiDungActivity.class);
            intent1.putExtra("maNguoiDung", getNguoiDung().getMaNguoiDung());
            startActivity(intent1);
            (requireActivity()).overridePendingTransition(R.anim.anim_in_right, R.anim.anim_out_left);
        } else if (view.getId() == R.id.cardDuyet) {
            // Mở màng hình quản lý hoá đơn
            startActivity(new Intent(getContext(), DuyetActivity.class));
            (requireActivity()).overridePendingTransition(R.anim.anim_in_right, R.anim.anim_out_left);
        } else if (view.getId() == R.id.cardMangVe) {
            Intent intent = new Intent(getContext(), MangVeActivity.class);
            intent.putExtra("maNguoiDung", getNguoiDung().getMaNguoiDung());
            Calendar c = Calendar.getInstance(); // lấy ngày thánh năm và giờ hiện tại
            HoaDonMangVeDao hoaDonMangVeDao = new HoaDonMangVeDao(getContext());
            if (!hoaDonMangVeDao.checkHoaDonChuaDuyet(getNguoiDung().getMaNguoiDung()) || !hoaDonMangVeDao.checkHoaDonChuaXacNhan(getNguoiDung().getMaNguoiDung())) {
                HoaDonMangVe hoaDonMangVe = new HoaDonMangVe();
                hoaDonMangVe.setMaKhachHang(getNguoiDung().getMaNguoiDung());
                hoaDonMangVe.setGioVao(c.getTime());
                hoaDonMangVe.setGioRa(c.getTime());
                hoaDonMangVe.setTrangThai(HoaDonMangVe.CHUA_XAC_NHAN);
                hoaDonMangVe.setGhiChu("");
                hoaDonMangVeDao.insertHoaDonMangVe(hoaDonMangVe);
            }
            startActivity(intent);
            (requireActivity()).overridePendingTransition(R.anim.anim_in_right, R.anim.anim_out_left);
        } else if (view.getId() == R.id.cardDoanhThu) {
            if (getNguoiDung().isAdmin()) {
                // Người dùng có chức vụ ="Admin" -> Mở màng hình quản lý doanh thu
                startActivity(new Intent(getContext(), DoanhThuActivity.class));
                (requireActivity()).overridePendingTransition(R.anim.anim_in_right, R.anim.anim_out_left);
            } else {
                // Người dùng có chức vụ = "NhanVien"
                MyToast.error(getContext(), "Chức năng dành cho Admin");
            }
        } else if (view.getId() == R.id.cardDatBan) {
            // Chỉ cho phép User (không phải Admin/NhanVien)
            if (!getNguoiDung().isAdmin() && !getNguoiDung().getChucVu().equals("NhanVien")) {
                startActivity(new Intent(getContext(), DatBanActivity.class));
                (requireActivity()).overridePendingTransition(R.anim.anim_in_right, R.anim.anim_out_left);
            } else {
                MyToast.error(getContext(), "Chức năng dành cho khách hàng");
            }
        } else if (view.getId() == R.id.cardQuanLyBanNguoiDung) {
            // Chỉ cho phép User (không phải Admin/NhanVien) - Xem các bàn đã đặt
            if (!getNguoiDung().isAdmin() && !getNguoiDung().getChucVu().equals("NhanVien")) {
                startActivity(new Intent(getContext(), QuanLyBanNguoiDungActivity.class));
                (requireActivity()).overridePendingTransition(R.anim.anim_in_right, R.anim.anim_out_left);
            } else {
                MyToast.error(getContext(), "Chức năng dành cho khách hàng");
            }
        }
    }

    /**
     * Hiển thị dialog chọn loại đặt hàng (Chọn bàn hoặc Mang về)
     */
    private void showChonLoaiDatHangDialog(HangHoa hangHoa) {
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.layout_dialog_chon_loai_dat_hang);
        
        TextView tvTenMon = dialog.findViewById(R.id.tvTenMon);
        Button btnChonBan = dialog.findViewById(R.id.btnChonBan);
        Button btnMangVe = dialog.findViewById(R.id.btnMangVe);
        TextView tvHuy = dialog.findViewById(R.id.tvHuy);
        
        // Hiển thị tên món
        tvTenMon.setText(hangHoa.getTenHangHoa());
        tvTenMon.setVisibility(View.VISIBLE);
        
        // Thiết lập dialog
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.90);
        int height = WindowManager.LayoutParams.WRAP_CONTENT;
        dialog.getWindow().setLayout(width, height);
        
        // Xử lý click Chọn bàn
        btnChonBan.setOnClickListener(v -> {
            dialog.dismiss();
            // Kiểm tra quyền - chỉ cho phép User (không phải Admin/NhanVien)
            NguoiDung nguoiDung = getNguoiDung();
            if (nguoiDung != null && !nguoiDung.isAdmin() && !nguoiDung.getChucVu().equals("NhanVien")) {
                // Hiển thị dialog chọn bàn đã đặt và đã duyệt
                showChonBanDialog(hangHoa);
            } else {
                MyToast.error(getContext(), "Chức năng dành cho khách hàng");
            }
        });
        
        // Xử lý click Mang về
        btnMangVe.setOnClickListener(v -> {
            dialog.dismiss();
            // Thêm món vào đơn mang về
            addDrinkToMangVe(hangHoa);
        });
        
        // Xử lý click Hủy
        tvHuy.setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
    }

    /**
     * Thêm món vào đơn mang về
     */
    private void addDrinkToMangVe(HangHoa hangHoa) {
        NguoiDung nguoiDung = getNguoiDung();
        if (nguoiDung == null) {
            MyToast.error(getContext(), "Không thể lấy thông tin người dùng");
            return;
        }
        
        HoaDonMangVeDao hoaDonMangVeDao = new HoaDonMangVeDao(getContext());
        Calendar c = Calendar.getInstance();
        
        // Lấy hoặc tạo hóa đơn mang về
        HoaDonMangVe hoaDonMangVe;
        try {
            hoaDonMangVe = hoaDonMangVeDao.getByMaHoaDonVaTrangThai(nguoiDung.getMaNguoiDung());
        } catch (Exception e) {
            // Chưa có hóa đơn, tạo mới
            hoaDonMangVe = new HoaDonMangVe();
            hoaDonMangVe.setMaKhachHang(nguoiDung.getMaNguoiDung());
            hoaDonMangVe.setGioVao(c.getTime());
            hoaDonMangVe.setGioRa(c.getTime());
            hoaDonMangVe.setTrangThai(HoaDonMangVe.CHUA_XAC_NHAN);
            hoaDonMangVe.setGhiChu("");
            hoaDonMangVeDao.insertHoaDonMangVe(hoaDonMangVe);
            // Lấy lại hóa đơn vừa tạo
            hoaDonMangVe = hoaDonMangVeDao.getByMaHoaDonVaTrangThai(nguoiDung.getMaNguoiDung());
        }
        
        // Thêm món vào hóa đơn
        HoaDonChiTiet hoaDonChiTiet = new HoaDonChiTiet();
        hoaDonChiTiet.setMaHoaDon(hoaDonMangVe.getMaHoaDon());
        hoaDonChiTiet.setMaHangHoa(hangHoa.getMaHangHoa());
        hoaDonChiTiet.setSoLuong(1);
        hoaDonChiTiet.setGiaTien(hangHoa.getGiaTien() * hoaDonChiTiet.getSoLuong());
        hoaDonChiTiet.setNgayXuatHoaDon(c.getTime());
        
        if (hoaDonChiTietDAO.insertHoaDonChiTiet(hoaDonChiTiet)) {
            MyToast.successful(getContext(), "Đã thêm " + hangHoa.getTenHangHoa() + " vào đơn mang về");
            // Mở màn hình mang về
            Intent intent = new Intent(getContext(), MangVeActivity.class);
            intent.putExtra("maNguoiDung", nguoiDung.getMaNguoiDung());
            startActivity(intent);
            requireActivity().overridePendingTransition(R.anim.anim_in_right, R.anim.anim_out_left);
        } else {
            MyToast.error(getContext(), "Thêm món thất bại");
        }
    }
    
    /**
     * Hiển thị dialog chọn bàn đã đặt và đã duyệt
     */
    private void showChonBanDialog(HangHoa hangHoa) {
        NguoiDung nguoiDung = getNguoiDung();
        if (nguoiDung == null) {
            MyToast.error(getContext(), "Không thể lấy thông tin người dùng");
            return;
        }
        
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.layout_dialog_chon_ban);
        
        RecyclerView recyclerViewBan = dialog.findViewById(R.id.recyclerViewBan);
        TextView tvEmpty = dialog.findViewById(R.id.tvEmpty);
        TextView tvHuy = dialog.findViewById(R.id.tvHuy);
        
        // Thiết lập dialog
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.90);
        int height = (int) (getResources().getDisplayMetrics().heightPixels * 0.80);
        dialog.getWindow().setLayout(width, height);
        
        // Load danh sách bàn đã đặt và đã duyệt
        datBanDAO.getByMaKhachHangDaDuyetFromFirebaseDirect(nguoiDung.getMaNguoiDung(), 
            new DatBanDAO.OnDatBanListListener() {
                @Override
                public void onListReceived(ArrayList<DatBan> datBanList) {
                    if (datBanList.isEmpty()) {
                        recyclerViewBan.setVisibility(View.GONE);
                        tvEmpty.setVisibility(View.VISIBLE);
                    } else {
                        recyclerViewBan.setVisibility(View.VISIBLE);
                        tvEmpty.setVisibility(View.GONE);
                        
                        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
                        recyclerViewBan.setLayoutManager(layoutManager);
                        
                        DatBanAdapter adapter = new DatBanAdapter(datBanList, new ItemDatBanOnClick() {
                            @Override
                            public void itemOclick(View view, DatBan datBan) {
                                dialog.dismiss();
                                // Thêm món vào đơn của bàn đã chọn
                                addDrinkToBan(hangHoa, datBan);
                            }
                        });
                        recyclerViewBan.setAdapter(adapter);
                    }
                }

                @Override
                public void onError(Exception e) {
                    MyToast.error(getContext(), "Không thể tải danh sách bàn");
                    dialog.dismiss();
                }
            });
        
        // Xử lý click Hủy
        tvHuy.setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
    }
    
    /**
     * Thêm món vào đơn của bàn đã chọn
     */
    private void addDrinkToBan(HangHoa hangHoa, DatBan datBan) {
        NguoiDung nguoiDung = getNguoiDung();
        if (nguoiDung == null) {
            MyToast.error(getContext(), "Không thể lấy thông tin người dùng");
            return;
        }
        
        // Lấy hoặc tạo hóa đơn cho bàn
        hoaDonDAO.getByMaBanFromFirebaseDirect(
            String.valueOf(datBan.getMaBan()), 
            datBan.getMaKhachHang(), 
            HoaDon.CHUA_THANH_TOAN,
            datBan.getNgayGioSuDung(),
            new HoaDonDAO.OnHoaDonListener() {
                @Override
                public void onHoaDonReceived(HoaDon hoaDon) {
                    // Đã có hóa đơn, thêm món vào
                    addDrinkToHoaDon(hangHoa, hoaDon, datBan);
                }

                @Override
                public void onError(Exception e) {
                    // Chưa có hóa đơn, tạo mới
                    createHoaDonAndAddDrink(hangHoa, datBan);
                }
            });
    }
    
    /**
     * Thêm món vào hóa đơn đã có
     */
    private void addDrinkToHoaDon(HangHoa hangHoa, HoaDon hoaDon, DatBan datBan) {
        Calendar c = Calendar.getInstance();
        HoaDonChiTiet hoaDonChiTiet = new HoaDonChiTiet();
        hoaDonChiTiet.setMaHoaDon(hoaDon.getMaHoaDon());
        hoaDonChiTiet.setMaHangHoa(hangHoa.getMaHangHoa());
        hoaDonChiTiet.setSoLuong(1);
        hoaDonChiTiet.setGiaTien(hangHoa.getGiaTien() * hoaDonChiTiet.getSoLuong());
        hoaDonChiTiet.setNgayXuatHoaDon(c.getTime());
        
        if (hoaDonChiTietDAO.insertHoaDonChiTiet(hoaDonChiTiet)) {
            MyToast.successful(getContext(), "Đã thêm " + hangHoa.getTenHangHoa() + " vào đơn bàn " + datBan.getMaBan());
            // Mở màn hình đặt món
            Intent intent = new Intent(getContext(), OderActivity.class);
            intent.putExtra(QuanLyBanActivity.MA_BAN, String.valueOf(datBan.getMaBan()));
            intent.putExtra("maKhachHang", datBan.getMaKhachHang());
            intent.putExtra("ngayGioSuDung", datBan.getNgayGioSuDung());
            startActivity(intent);
            requireActivity().overridePendingTransition(R.anim.anim_in_right, R.anim.anim_out_left);
        } else {
            MyToast.error(getContext(), "Thêm món thất bại");
        }
    }
    
    /**
     * Tạo hóa đơn mới và thêm món
     */
    private void createHoaDonAndAddDrink(HangHoa hangHoa, DatBan datBan) {
        // Tạo hóa đơn mới từ DatBan
        Calendar c = Calendar.getInstance();
        HoaDon hoaDon = new HoaDon();
        hoaDon.setMaBan(datBan.getMaBan());
        hoaDon.setMaKhachHang(datBan.getMaKhachHang());
        hoaDon.setTrangThai(HoaDon.CHUA_THANH_TOAN);
        hoaDon.setGhiChu(datBan.getGhiChu());
        
        // Parse ngayGioSuDung từ DatBan
        try {
            hoaDon.setGioVao(XDate.toDateTime(datBan.getNgayGioSuDung()));
            hoaDon.setGioRa(XDate.toDateTime(datBan.getNgayGioSuDung()));
        } catch (Exception e) {
            hoaDon.setGioVao(c.getTime());
            hoaDon.setGioRa(c.getTime());
        }
        
        if (hoaDonDAO.insertHoaDon(hoaDon)) {
            // Lấy lại hóa đơn vừa tạo để có maHoaDon
            // Chờ một chút để Firebase sync
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                hoaDonDAO.getByMaBanFromFirebaseDirect(
                    String.valueOf(datBan.getMaBan()),
                    datBan.getMaKhachHang(),
                    HoaDon.CHUA_THANH_TOAN,
                    datBan.getNgayGioSuDung(),
                    new HoaDonDAO.OnHoaDonListener() {
                        @Override
                        public void onHoaDonReceived(HoaDon newHoaDon) {
                            addDrinkToHoaDon(hangHoa, newHoaDon, datBan);
                        }

                        @Override
                        public void onError(Exception e) {
                            MyToast.error(getContext(), "Không thể tạo hóa đơn");
                        }
                    });
            }, 500);
        } else {
            MyToast.error(getContext(), "Không thể tạo hóa đơn");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        welcomeUser();
        loadListThucUong();
    }
}