package app.edu.app.fragment;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Objects;

import app.edu.app.MainActivity;
import app.edu.app.R;
import app.edu.app.adapter.DatBanAdapter;
import app.edu.app.adapter.LoaiHangAdapter;
import app.edu.app.adapter.SearchDrinkAdapter;
import app.edu.app.dao.DatBanDAO;
import app.edu.app.dao.HangHoaDAO;
import app.edu.app.dao.HoaDonChiTietDAO;
import app.edu.app.dao.HoaDonDAO;
import app.edu.app.dao.HoaDonMangVeDao;
import app.edu.app.dao.LoaiHangDAO;
import app.edu.app.dao.NguoiDungDAO;
import app.edu.app.interfaces.ItemDatBanOnClick;
import app.edu.app.interfaces.ItemHangHoaOnClick;
import app.edu.app.interfaces.ItemLoaiHangOnClick;
import app.edu.app.model.DatBan;
import app.edu.app.model.HangHoa;
import app.edu.app.model.HoaDon;
import app.edu.app.model.HoaDonChiTiet;
import app.edu.app.model.HoaDonMangVe;
import app.edu.app.model.LoaiHang;
import app.edu.app.model.NguoiDung;
import app.edu.app.ui.MangVeActivity;
import app.edu.app.ui.OderActivity;
import app.edu.app.ui.QuanLyBanActivity;
import app.edu.app.ui.ThucUongActivity;
import app.edu.app.utils.MyToast;
import app.edu.app.utils.XDate;

public class SearchFragment extends Fragment {
    
    private EditText editSearch;
    private RecyclerView recyclerViewCategories;
    private RecyclerView recyclerViewDrinks;
    private TextView tag1, tag2, tag3, tag4, tag5, tag6, tag7, tag8, tag9;
    
    private HangHoaDAO hangHoaDAO;
    private LoaiHangDAO loaiHangDAO;
    private MainActivity mainActivity;
    private NguoiDungDAO nguoiDungDAO;
    private DatBanDAO datBanDAO;
    private HoaDonDAO hoaDonDAO;
    private HoaDonChiTietDAO hoaDonChiTietDAO;
    
    private LoaiHangAdapter loaiHangAdapter;
    private SearchDrinkAdapter searchDrinkAdapter;
    
    private ArrayList<HangHoa> allDrinks;
    private ArrayList<HangHoa> filteredDrinks;
    
    private String[] popularSearches = {
        "Trà sữa", "Cà phê", "Trà trái cây", 
        "Sinh tố", "Soda", "Macchiato",
        "Trà chanh", "Matcha", "Chocolate"
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);
        
        initView(view);
        initDAO();
        setupPopularSearchChips();
        setupRecyclerViews();
        loadCategories();
        loadDrinks();
        setupSearchListener();
        
        return view;
    }

    private void initView(View view) {
        editSearch = view.findViewById(R.id.editSearch);
        recyclerViewCategories = view.findViewById(R.id.recyclerViewCategories);
        recyclerViewDrinks = view.findViewById(R.id.recyclerViewDrinks);
        
        // Init popular search tags
        tag1 = view.findViewById(R.id.tag1);
        tag2 = view.findViewById(R.id.tag2);
        tag3 = view.findViewById(R.id.tag3);
        tag4 = view.findViewById(R.id.tag4);
        tag5 = view.findViewById(R.id.tag5);
        tag6 = view.findViewById(R.id.tag6);
        tag7 = view.findViewById(R.id.tag7);
        tag8 = view.findViewById(R.id.tag8);
        tag9 = view.findViewById(R.id.tag9);
    }
    
    private void initDAO() {
        hangHoaDAO = new HangHoaDAO(getContext());
        loaiHangDAO = new LoaiHangDAO(getContext());
        nguoiDungDAO = new NguoiDungDAO(getContext());
        datBanDAO = new DatBanDAO(getContext());
        hoaDonDAO = new HoaDonDAO(getContext());
        hoaDonChiTietDAO = new HoaDonChiTietDAO(getContext());
        mainActivity = (MainActivity) getActivity();
        allDrinks = new ArrayList<>();
        filteredDrinks = new ArrayList<>();
    }
    
    /**
     * Setup popular search tags
     */
    private void setupPopularSearchChips() {
        TextView[] tags = {tag1, tag2, tag3, tag4, tag5, tag6, tag7, tag8, tag9};
        
        for (TextView tag : tags) {
            if (tag != null) {
                tag.setOnClickListener(v -> {
                    String searchTerm = tag.getText().toString();
                    editSearch.setText(searchTerm);
                    performSearch(searchTerm);
                });
            }
        }
    }
    
    /**
     * Setup RecyclerViews
     */
    private void setupRecyclerViews() {
        // Categories RecyclerView (Horizontal)
        LinearLayoutManager categoriesLayoutManager = new LinearLayoutManager(
            getContext(), 
            LinearLayoutManager.HORIZONTAL, 
            false
        );
        recyclerViewCategories.setLayoutManager(categoriesLayoutManager);
        
        // Drinks RecyclerView (Grid 2 columns)
        GridLayoutManager drinksLayoutManager = new GridLayoutManager(getContext(), 2);
        recyclerViewDrinks.setLayoutManager(drinksLayoutManager);
    }
    
    /**
     * Load categories
     */
    private void loadCategories() {
        ArrayList<LoaiHang> loaiHangList = loaiHangDAO.getAll();
        loaiHangAdapter = new LoaiHangAdapter(loaiHangList, new ItemLoaiHangOnClick() {
            @Override
            public void itemOclick(View view, LoaiHang loaiHang) {
                // Mở ThucUongActivity
                Intent intent = new Intent(getContext(), ThucUongActivity.class);
                startActivity(intent);
                requireActivity().overridePendingTransition(R.anim.anim_in_right, R.anim.anim_out_left);
            }
        });
        recyclerViewCategories.setAdapter(loaiHangAdapter);
    }
    
    /**
     * Load drinks
     */
    private void loadDrinks() {
        ArrayList<HangHoa> hangHoaList = hangHoaDAO.getAll();
        allDrinks.clear();
        allDrinks.addAll(hangHoaList);
        
        // Hiển thị tất cả sản phẩm ban đầu
        filteredDrinks.clear();
        filteredDrinks.addAll(allDrinks);
        
        updateDrinksAdapter();
    }
    
    /**
     * Update drinks adapter with filtered list
     */
    private void updateDrinksAdapter() {
        searchDrinkAdapter = new SearchDrinkAdapter(filteredDrinks, new ItemHangHoaOnClick() {
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
        recyclerViewDrinks.setAdapter(searchDrinkAdapter);
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
            // Hiển thị dialog chọn bàn đã đặt và đã duyệt
            showChonBanDialog(hangHoa);
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
     * Lấy thông tin người dùng hiện tại
     */
    private NguoiDung getNguoiDung() {
        if (mainActivity == null) {
            return null;
        }
        String maNguoiDung = mainActivity.getKeyUser();
        if (maNguoiDung == null || maNguoiDung.isEmpty()) {
            return null;
        }
        return nguoiDungDAO.getByMaNguoiDung(maNguoiDung);
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
    
    /**
     * Setup search listener
     */
    private void setupSearchListener() {
        editSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                performSearch(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }
    
    /**
     * Perform search on drinks list
     */
    private void performSearch(String query) {
        filteredDrinks.clear();
        
        if (query.isEmpty()) {
            // Nếu không có query, hiển thị tất cả
            filteredDrinks.addAll(allDrinks);
        } else {
            // Lọc theo tên
            String lowerCaseQuery = query.toLowerCase().trim();
            for (HangHoa hangHoa : allDrinks) {
                if (hangHoa.getTenHangHoa().toLowerCase().contains(lowerCaseQuery)) {
                    filteredDrinks.add(hangHoa);
                }
            }
        }
        
        updateDrinksAdapter();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Reload data when fragment is resumed
        loadCategories();
        loadDrinks();
    }
}
