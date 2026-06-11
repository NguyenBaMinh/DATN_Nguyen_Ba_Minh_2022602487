package app.edu.app.ui;

import static app.edu.app.ui.SignInActivity._maNguoiDung;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.ArrayList;
import java.util.Calendar;

import app.edu.app.R;
import app.edu.app.adapter.HoaDonChiTietMainAdapter;
import app.edu.app.dao.BanDAO;
import app.edu.app.dao.HangHoaDAO;
import app.edu.app.dao.HoaDonChiTietDAO;
import app.edu.app.dao.HoaDonDAO;
import app.edu.app.dao.NguoiDungDAO;
import app.edu.app.dao.ThongBaoDAO;
import app.edu.app.interfaces.ItemTangGiamSoLuongOnClick;
import app.edu.app.model.Ban;
import app.edu.app.model.HangHoa;
import app.edu.app.model.HoaDon;
import app.edu.app.model.HoaDonChiTiet;
import app.edu.app.model.ThongBao;
import app.edu.app.utils.MyToast;
import app.edu.app.utils.VNPayHelper;
import app.edu.app.utils.XDate;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

public class OderActivity extends AppCompatActivity {
    public static final String MA_HOA_DON = "maHoaDon";
    HoaDonChiTietDAO hoaDonChiTietDAO;
    HangHoaDAO hangHoaDAO;
    HoaDonDAO hoaDonDAO;
    BanDAO banDAO;
    TextView tvMaBan, tvGioVao, tvThemMon, tvTamTinh, tvHoaDonCuoi, tvnguoi, tvAISuggestion;
    RecyclerView recyclerViewThucUong;
    SwipeRefreshLayout swipeRefresh;
    Button btnThanhToan;
    Toolbar toolbar;
    public static String maBan = "";
    private SharedPreferences sharedPreferences;
    private AISuggestionDialog aiSuggestionDialog;
    private boolean isFirstLoad = true;
    private HoaDonChiTietMainAdapter adapter;
    private HoaDon currentHoaDon;
    private boolean isLoadingData = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_oder);
        initToolbar();
        initview();

        hoaDonChiTietDAO = new HoaDonChiTietDAO(this);
        hangHoaDAO = new HangHoaDAO(this);
        hoaDonDAO = new HoaDonDAO(this);
        banDAO = new BanDAO(this);

        sharedPreferences = getSharedPreferences("USER_FILE", MODE_PRIVATE);
        loadHoaDonFromFirebase();

        tvThemMon.setOnClickListener(view -> openThemMonActivity());
        btnThanhToan.setOnClickListener(view -> thanhToanHoaDon());
    }

    private void loadHoaDonFromFirebase() {
        Intent intent = getIntent();
        if (maBan.equals("")) {
            maBan = intent.getStringExtra("maBan");
        }

        String maKhachHangFromIntent = intent.getStringExtra("maKhachHang");
        String currentMaKhachHang = sharedPreferences.getString("maNguoiDung", "");

        String maKhachHang = null;
        if (maKhachHangFromIntent != null && !maKhachHangFromIntent.isEmpty()) {
            maKhachHang = maKhachHangFromIntent;
        } else if (currentMaKhachHang != null && !currentMaKhachHang.isEmpty()) {
            maKhachHang = currentMaKhachHang;
        }

        String ngayGioSuDung = intent.getStringExtra("ngayGioSuDung");
        String finalMaKhachHang = maKhachHang;

        hoaDonDAO.getByMaBanFromFirebaseDirect(maBan, maKhachHang, HoaDon.CHUA_THANH_TOAN, ngayGioSuDung,
                new HoaDonDAO.OnHoaDonListener() {
                    @Override
                    public void onHoaDonReceived(HoaDon hoaDon) {
                        currentHoaDon = hoaDon;
                        String maHoaDon = String.valueOf(hoaDon.getMaHoaDon());
                        aiSuggestionDialog = new AISuggestionDialog(OderActivity.this, maHoaDon);
                        loadData();
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.w("OderActivity", "Không tìm thấy hóa đơn: " + e.getMessage());
                        MyToast.error(OderActivity.this, "Bàn này chưa có hóa đơn. Vui lòng tạo hóa đơn mới từ Quản lý bàn.");
                        finish();
                    }
                });
    }

    private void initToolbar() {
        toolbar = findViewById(R.id.toolbarOder);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(view -> onBackPressed());
    }

    private void initview() {
        tvMaBan = findViewById(R.id.tvMaBan);
        tvGioVao = findViewById(R.id.tvGioVao);
        recyclerViewThucUong = findViewById(R.id.recyclerViewThucUong);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        tvThemMon = findViewById(R.id.tvThemMon);
        btnThanhToan = findViewById(R.id.btnThanhToan);
        tvTamTinh = findViewById(R.id.tvTamTinh);
        tvHoaDonCuoi = findViewById(R.id.tvHoaDonCuoi);
        tvnguoi = findViewById(R.id.tvnguoidat);
        tvAISuggestion = findViewById(R.id.tvAISuggestion);

        swipeRefresh.setColorSchemeResources(R.color.BluePrimary, R.color.RedPrimary, R.color.GreenPrimary);
        swipeRefresh.setOnRefreshListener(() -> {
            if (currentHoaDon != null) {
                loadData();
                fillActivity();
            } else {
                swipeRefresh.setRefreshing(false);
            }
        });

        tvAISuggestion.setOnClickListener(view -> {
            if (aiSuggestionDialog == null) {
                HoaDon hoaDon = getHoaDon();
                String maHoaDon = hoaDon != null ? String.valueOf(hoaDon.getMaHoaDon()) : null;
                aiSuggestionDialog = new AISuggestionDialog(OderActivity.this, maHoaDon);
            }
            aiSuggestionDialog.show();
        });
    }

//    private void openThemMonActivity() {
//        HoaDon hoaDon = getHoaDon();
//        if (hoaDon == null || hoaDon.getMaHoaDon() == 0) {
//            MyToast.error(this, "Không có hóa đơn để thêm món.");
//            return;
//        }
//        Intent intent = new Intent(OderActivity.this, ThemMonActivity.class);
//        intent.putExtra(MA_HOA_DON, String.valueOf(hoaDon.getMaHoaDon()));
//        startActivity(intent);
//        finish();
//    }
private void openThemMonActivity() {
    HoaDon hoaDon = getHoaDon();

    if (hoaDon == null || hoaDon.getMaHoaDon() == 0) {
        MyToast.error(this, "Không có hóa đơn để thêm món. Vui lòng tạo hóa đơn mới từ Quản lý bàn.");
        return;
    }

    Intent intent = new Intent(OderActivity.this, ThemMonActivity.class);
    intent.putExtra(MA_HOA_DON, String.valueOf(hoaDon.getMaHoaDon()));
    intent.putExtra("maBan", String.valueOf(hoaDon.getMaBan())); // ✅ Thêm dòng này
    startActivity(intent);
    finish();
}

    @SuppressLint("SetTextI18n")
    private void thanhToanHoaDon() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.layout_dialog_thanhtoan);

        TextView tvMahoaDon = dialog.findViewById(R.id.tvMaHoaDon);
        TextView tvMaBanDlg = dialog.findViewById(R.id.tvMaBan);
        TextView tvGioVaoTT = dialog.findViewById(R.id.tvGioVao);
        TextView tvHoaDonCuoiDlg = dialog.findViewById(R.id.tvHoaDonCUoi);
        TextView tvnguoiDlg = dialog.findViewById(R.id.tvnguoidat);
        TextView tvTongTien = dialog.findViewById(R.id.tvTongTien);
        TextView tvCancle = dialog.findViewById(R.id.tvCancle);
        EditText edtGhiChu = dialog.findViewById(R.id.edtGhiChu);
        Button btnPay = dialog.findViewById(R.id.btnPay);
        Button btnPayVNPay = dialog.findViewById(R.id.btnPayVNPay);

        HoaDon hoaDonForDialog = getHoaDon();
        tvMahoaDon.setText("HD0" + hoaDonForDialog.getMaHoaDon());
        tvMaBanDlg.setText("B0" + hoaDonForDialog.getMaBan());
        tvGioVaoTT.setText(XDate.toStringDateTime(hoaDonForDialog.getGioVao()));

        // Load tổng tiền
        hoaDonChiTietDAO.getByMaHoaDonFromFirebaseDirect(hoaDonForDialog.getMaHoaDon(),
                new HoaDonChiTietDAO.OnHoaDonChiTietListListener() {
                    @Override
                    public void onListReceived(ArrayList<HoaDonChiTiet> listHDCT) {
                        long tongTien = 0;
                        for (HoaDonChiTiet hdct : listHDCT) tongTien += hdct.getGiaTien();
                        final long finalTongTien = tongTien;
                        runOnUiThread(() -> {
                            tvTongTien.setText(finalTongTien + "VND");
                            tvHoaDonCuoiDlg.setText(finalTongTien + "VND");
                        });
                    }
                    @Override
                    public void onError(Exception e) {
                        runOnUiThread(() -> {
                            tvTongTien.setText("0VND");
                            tvHoaDonCuoiDlg.setText("0VND");
                        });
                    }
                });

        // Load thông tin người dùng
        NguoiDungDAO nguoiDungDAO = new NguoiDungDAO(this);
        String ma = sharedPreferences.getString("maNguoiDung", "");
        nguoiDungDAO.getByMaNguoiDungFromFirebaseDirect(ma, new NguoiDungDAO.OnNguoiDungListener() {
            @Override
            public void onNguoiDungReceived(app.edu.app.model.NguoiDung nguoiDung) {
                runOnUiThread(() -> tvnguoiDlg.setText(nguoiDung.getHoVaTen()));
            }
            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> tvnguoiDlg.setText("Khách hàng"));
            }
        });

        tvCancle.setOnClickListener(view -> dialog.dismiss());

        // =============================================
        // THANH TOÁN TIỀN MẶT — Update thẳng Firebase
        // =============================================
        btnPay.setOnClickListener(view -> {
            HoaDon hoaDon = getHoaDon();
            hoaDon.setTrangThai(HoaDon.DA_THANH_TOAN);
            Calendar calendar = Calendar.getInstance();
            hoaDon.setGioRa(calendar.getTime());
            hoaDon.setGhiChu(edtGhiChu.getText().toString());
            _maNguoiDung = sharedPreferences.getString("maNguoiDung", "");
            hoaDon.setMaKhachHang(_maNguoiDung);

            Intent intentGet = getIntent();
            String maBanStr = intentGet.getStringExtra(QuanLyBanActivity.MA_BAN);

            // ✅ Update hóa đơn TRỰC TIẾP lên Firebase trước
            hoaDonDAO.updateHoaDonToFirebaseDirect(hoaDon,
                    aVoid -> {
                        Log.d("OderActivity", "✅ Đã update hóa đơn lên Firebase: trangThai=" + hoaDon.getTrangThai());

                        // Sau đó update bàn
                        banDAO.getByMaBanFromFirebaseDirect(maBanStr, new BanDAO.OnBanListener() {
                            @Override
                            public void onBanReceived(Ban ban) {
                                ban.setTrangThai(Ban.CON_TRONG);

                                // Mở ChuyenKhoanActivity với dữ liệu đã update
                                Intent intent1 = new Intent(OderActivity.this, ChuyenKhoanActivity.class);
                                intent1.putExtra("ban", ban);
                                intent1.putExtra("hoaDon", hoaDon);
                                dialog.dismiss();
                                startActivity(intent1);
                                finish();
                            }

                            @Override
                            public void onError(Exception e) {
                                Log.e("OderActivity", "Lỗi load bàn", e);
                                MyToast.error(OderActivity.this, "Không thể load thông tin bàn");
                            }
                        });
                    },
                    e -> {
                        Log.e("OderActivity", "❌ Lỗi update hóa đơn lên Firebase", e);
                        MyToast.error(OderActivity.this, "Lỗi cập nhật hóa đơn: " + e.getMessage());
                    }
            );
        });

        // =============================================
        // THANH TOÁN VNPAY
        // =============================================
        btnPayVNPay.setOnClickListener(view -> {
            HoaDon hoaDon = getHoaDon();
            String orderId = "HD0" + hoaDon.getMaHoaDon();
            String orderInfo = "Thanh toan hoa don " + orderId;
            String ipAddr = getLocalIpAddress();

            hoaDonChiTietDAO.getByMaHoaDonFromFirebaseDirect(hoaDon.getMaHoaDon(),
                    new HoaDonChiTietDAO.OnHoaDonChiTietListListener() {
                        @Override
                        public void onListReceived(ArrayList<HoaDonChiTiet> listHDCT) {
                            long totalAmount = 0;
                            for (HoaDonChiTiet hdct : listHDCT) totalAmount += hdct.getGiaTien();
                            final long finalAmount = totalAmount;
                            runOnUiThread(() -> {
                                String paymentUrl = VNPayHelper.createPaymentUrl(orderId, finalAmount, orderInfo, ipAddr, null);
                                if (paymentUrl != null && !paymentUrl.isEmpty()) {
                                    dialog.dismiss();
                                    Intent vnpayIntent = new Intent(OderActivity.this, VNPayPaymentActivity.class);
                                    vnpayIntent.putExtra("payment_url", paymentUrl);
                                    vnpayIntent.putExtra("order_id", orderId);
                                    vnpayIntent.putExtra("amount", finalAmount);
                                    vnpayIntent.putExtra("hoa_don_id", String.valueOf(hoaDon.getMaHoaDon()));
                                    startActivityForResult(vnpayIntent, 1001);
                                } else {
                                    MyToast.error(OderActivity.this, "Không thể tạo link thanh toán");
                                }
                            });
                        }
                        @Override
                        public void onError(Exception e) {
                            MyToast.error(OderActivity.this, "Không thể tính tổng tiền");
                        }
                    });
        });

        dialog.show();
        dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
        dialog.getWindow().setGravity(Gravity.BOTTOM);
    }

    private void themThonBaoMoi(HoaDon hoaDon, Calendar calendar) {
        ThongBao thongBao = new ThongBao();
        thongBao.setNoiDung("Thanh toán thành công hoá đơn HD0" + hoaDon.getMaHoaDon());
        thongBao.setTrangThai(ThongBao.STATUS_CHUA_XEM);
        thongBao.setNgayThongBao(calendar.getTime());
        ThongBaoDAO thongBaoDAO = new ThongBaoDAO(OderActivity.this);
        thongBaoDAO.insertThongBao(thongBao);
    }

    private void loadData() {
        HoaDon hoaDon = getHoaDon();
        if (hoaDon == null || hoaDon.getMaHoaDon() == 0) {
            MyToast.error(this, "Bàn này chưa có hóa đơn.");
            finish();
            return;
        }

        hoaDonChiTietDAO.getByMaHoaDonFromFirebaseDirect(hoaDon.getMaHoaDon(),
                new HoaDonChiTietDAO.OnHoaDonChiTietListListener() {
                    @Override
                    public void onListReceived(ArrayList<HoaDonChiTiet> listHDCT) {
                        if (listHDCT.isEmpty()) {
                            swipeRefresh.setRefreshing(false);
                            setupRecyclerView(new ArrayList<>(), new ArrayList<>());
                            return;
                        }

                        final ArrayList<HangHoa> listHangHoa = new ArrayList<>(listHDCT.size());
                        for (int i = 0; i < listHDCT.size(); i++) listHangHoa.add(null);

                        final int[] loadedCount = {0};
                        final int[] errorCount = {0};

                        for (int i = 0; i < listHDCT.size(); i++) {
                            final int index = i;
                            final HoaDonChiTiet hdct = listHDCT.get(i);

                            hangHoaDAO.getByMaHangHoaFromFirebaseDirect(hdct.getMaHangHoa(),
                                    new HangHoaDAO.OnHangHoaListener() {
                                        @Override
                                        public void onHangHoaReceived(HangHoa hangHoa) {
                                            listHangHoa.set(index, hangHoa);
                                            loadedCount[0]++;
                                            if (loadedCount[0] == listHDCT.size()) buildAndShowList(listHangHoa, listHDCT);
                                        }
                                        @Override
                                        public void onError(Exception e) {
                                            loadedCount[0]++;
                                            errorCount[0]++;
                                            if (loadedCount[0] == listHDCT.size()) buildAndShowList(listHangHoa, listHDCT);
                                        }
                                    });
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        MyToast.error(OderActivity.this, "Không thể load dữ liệu từ Firebase");
                        swipeRefresh.setRefreshing(false);
                    }
                });
    }

    private void buildAndShowList(ArrayList<HangHoa> listHangHoa, ArrayList<HoaDonChiTiet> listHDCT) {
        ArrayList<HangHoa> filteredHangHoa = new ArrayList<>();
        ArrayList<HoaDonChiTiet> filteredHDCT = new ArrayList<>();
        for (int j = 0; j < listHangHoa.size(); j++) {
            if (listHangHoa.get(j) != null) {
                filteredHangHoa.add(listHangHoa.get(j));
                filteredHDCT.add(listHDCT.get(j));
            }
        }
        setupRecyclerView(filteredHangHoa, filteredHDCT);
        runOnUiThread(() -> {
            fillActivity();
            swipeRefresh.setRefreshing(false);
        });
    }

    private void setupRecyclerView(ArrayList<HangHoa> listHangHoa, ArrayList<HoaDonChiTiet> listHDCT) {
        if (adapter != null) {
            adapter.updateData(listHangHoa, listHDCT);
            runOnUiThread(() -> {
                if (recyclerViewThucUong.getAdapter() != adapter) recyclerViewThucUong.setAdapter(adapter);
                adapter.notifyDataSetChanged();
            });
            return;
        }

        if (recyclerViewThucUong.getLayoutManager() == null) {
            recyclerViewThucUong.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        }

        adapter = new HoaDonChiTietMainAdapter(this, listHangHoa, listHDCT,
                new ItemTangGiamSoLuongOnClick() {
                    @Override
                    public void itemOclick(View view, int indext, HoaDonChiTiet hoaDonChiTiet, HangHoa hangHoa) {
                        hoaDonChiTiet.setSoLuong(indext);
                        hoaDonChiTiet.setGiaTien(indext * hangHoa.getGiaTien());
                        hoaDonChiTietDAO.updateHoaDonChiTiet(hoaDonChiTiet);
                        fillActivity();
                    }

                    @Override
                    public void itemOclickDeleteHDCT(View view, HoaDonChiTiet hoaDonChiTiet) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(OderActivity.this, R.style.AlertDialogTheme);
                        builder.setMessage("Xoá món này?");
                        builder.setPositiveButton("Xoá", (dialogInterface, i) -> {
                            if (hoaDonChiTietDAO.deleteHoaDonChiTiet(String.valueOf(hoaDonChiTiet.getMaHDCT()))) {
                                MyToast.successful(OderActivity.this, "Xoá món thành công");
                            } else {
                                MyToast.error(OderActivity.this, "Xoá không thành công");
                            }
                        });
                        builder.setNegativeButton("Huỷ", (dialogInterface, i) -> dialogInterface.dismiss());
                        builder.create().show();
                    }
                });

        recyclerViewThucUong.setAdapter(adapter);
    }

    @SuppressLint("SetTextI18n")
    private void fillActivity() {
        HoaDon hoaDon = getHoaDon();
        if (hoaDon == null || hoaDon.getMaHoaDon() == 0) {
            tvMaBan.setText("Bàn BO" + maBan);
            tvGioVao.setText("-");
            tvTamTinh.setText("0VND");
            tvHoaDonCuoi.setText("0VND");
            return;
        }

        tvMaBan.setText("Bàn BO" + hoaDon.getMaBan());
        tvGioVao.setText(XDate.toStringDateTime(hoaDon.getGioVao()));

        hoaDonChiTietDAO.getByMaHoaDonFromFirebaseDirect(hoaDon.getMaHoaDon(),
                new HoaDonChiTietDAO.OnHoaDonChiTietListListener() {
                    @Override
                    public void onListReceived(ArrayList<HoaDonChiTiet> listHDCT) {
                        long tongTien = 0;
                        for (HoaDonChiTiet hdct : listHDCT) tongTien += hdct.getGiaTien();
                        final long finalTongTien = tongTien;
                        runOnUiThread(() -> {
                            tvTamTinh.setText(finalTongTien + "VND");
                            tvHoaDonCuoi.setText(finalTongTien + "VND");
                        });
                    }
                    @Override
                    public void onError(Exception e) {
                        runOnUiThread(() -> {
                            tvTamTinh.setText("0VND");
                            tvHoaDonCuoi.setText("0VND");
                        });
                    }
                });
    }

    private HoaDon getHoaDon() {
        return currentHoaDon;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isFirstLoad) {
            Log.d("OderActivity", "onResume: real-time listener active");
        } else {
            isFirstLoad = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        hoaDonDAO.stopFirebaseDirectListener();
        hoaDonChiTietDAO.stopFirebaseDirectListener();
        hangHoaDAO.stopFirebaseDirectListener();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1001) {
            if (resultCode == RESULT_OK && data != null) {
                boolean success = data.getBooleanExtra("success", false);
                String message = data.getStringExtra("message");

                if (success) {
                    HoaDon hoaDon = getHoaDon();
                    hoaDon.setTrangThai(HoaDon.DA_THANH_TOAN);
                    Calendar calendar = Calendar.getInstance();
                    hoaDon.setGioRa(calendar.getTime());
                    _maNguoiDung = sharedPreferences.getString("maNguoiDung", "");
                    hoaDon.setMaKhachHang(_maNguoiDung);

                    Intent intent = getIntent();
                    String maBanStr = intent.getStringExtra(QuanLyBanActivity.MA_BAN);

                    // ✅ Update hóa đơn TRỰC TIẾP lên Firebase
                    hoaDonDAO.updateHoaDonToFirebaseDirect(hoaDon,
                            aVoid -> {
                                Log.d("OderActivity", "✅ VNPay: Đã update hóa đơn lên Firebase");
                                banDAO.getByMaBanFromFirebaseDirect(maBanStr, new BanDAO.OnBanListener() {
                                    @Override
                                    public void onBanReceived(Ban ban) {
                                        ban.setTrangThai(Ban.CON_TRONG);
                                        banDAO.updateBan(ban);
                                        MyToast.successful(OderActivity.this, message);
                                        themThonBaoMoi(hoaDon, calendar);
                                        Intent intent1 = new Intent(OderActivity.this, QuanLyBanActivity.class);
                                        OderActivity.maBan = "";
                                        startActivity(intent1);
                                        finish();
                                    }
                                    @Override
                                    public void onError(Exception e) {
                                        Log.e("OderActivity", "Lỗi load bàn VNPay", e);
                                        MyToast.error(OderActivity.this, "Không thể load thông tin bàn");
                                    }
                                });
                            },
                            e -> {
                                Log.e("OderActivity", "❌ VNPay: Lỗi update hóa đơn", e);
                                MyToast.error(OderActivity.this, "Lỗi cập nhật hóa đơn: " + e.getMessage());
                            }
                    );
                } else {
                    MyToast.error(OderActivity.this, message);
                }
            }
        }
    }

    private String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof java.net.Inet4Address) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
            Log.e("IPAddress", "Error getting IP address", e);
        }
        return "127.0.0.1";
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.anim_in_left, R.anim.anim_out_right);
    }
}