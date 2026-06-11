package app.edu.app.ui;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;
import app.edu.app.R;
import app.edu.app.dao.NguoiDungDAO;
import app.edu.app.interfaces.OnUrlFetchedListener;
import app.edu.app.model.NguoiDung;
import app.edu.app.utils.ImageCache;
import app.edu.app.utils.XDate;

public class ChiTietNhanVienActivity extends AppCompatActivity {
    ImageView ivBack;
    TextView tvTenNhanVien, tvEmailNhanVien, tvHoVaTen, tvNgaySinh, tvEmail, tvGioiTinh, tvChucVu, tvMaNguoiDung, tvMatKhau;
    NguoiDungDAO nguoiDungDAO;
    CircleImageView civHinhAnh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chi_tiet_nhan_vien);
        initStatusBar();
        initView();
        nguoiDungDAO = new NguoiDungDAO(this);
        ivBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        fillData();
    }

    private void fillData() {
        NguoiDung nguoiDung = getNguoiDung();
//
//        Bitmap bitmap = BitmapFactory.decodeByteArray(nguoiDung.getHinhAnh(),
//                0,
//                nguoiDung.getHinhAnh().length);
        ImageCache.getUrlFromCache("nguoidung_" + nguoiDung.getMaNguoiDung(), new OnUrlFetchedListener() {
            @Override
            public void onUrlFetched(String url) {
                Picasso.get().load(url).into(civHinhAnh);
            }
        });
        tvTenNhanVien.setText(nguoiDung.getHoVaTen());
        tvEmailNhanVien.setText(nguoiDung.getEmail());
        tvHoVaTen.setText(nguoiDung.getHoVaTen());
        tvNgaySinh.setText(XDate.toStringDate(nguoiDung.getNgaySinh()));
        tvEmail.setText(nguoiDung.getEmail());
        tvGioiTinh.setText(getGender(nguoiDung));
        tvChucVu.setText(getPosition(nguoiDung));
        tvMaNguoiDung.setText(nguoiDung.getMaNguoiDung());
        tvMatKhau.setText(nguoiDung.getMatKhau());

    }

    private String getPosition(NguoiDung nguoiDung) {
        if (nguoiDung.getChucVu().equals(NguoiDung.POSITION_ADMIN)) {
            return NguoiDung.POSITION_ADMIN;
        }
        return NguoiDung.POSITION_STAFF;
    }

    private String getGender(NguoiDung nguoiDung) {
        if (nguoiDung.getGioiTinh().equals(NguoiDung.GENDER_MALE)) {
            return "Nam";
        }
        return "Nữ";
    }

    private NguoiDung getNguoiDung() {
        // Lấy đối tượng người dùng
        Intent intent = getIntent();
        return nguoiDungDAO.getByMaNguoiDung(intent.getStringExtra(NhanVienActivity.MA_NGUOI_DUNG));
    }

    private void initView() {
        ivBack = findViewById(R.id.ivBack);
        tvTenNhanVien = findViewById(R.id.tvTenNhanVien);
        tvEmailNhanVien = findViewById(R.id.tvEmailNhanVien);
        tvHoVaTen = findViewById(R.id.tvHovaTen);
        tvNgaySinh = findViewById(R.id.tvNgaySinh);
        tvEmail = findViewById(R.id.tvEmail);
        tvGioiTinh = findViewById(R.id.tvGioiTinh);
        tvChucVu = findViewById(R.id.tvChucVu);
        tvMaNguoiDung = findViewById(R.id.tvMaNguoiDung);
        tvMatKhau = findViewById(R.id.tvMatKhau);
        civHinhAnh = findViewById(R.id.civHinhAnh);
    }

    private void initStatusBar() {
        // Status bar
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        );
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.anim_in_left, R.anim.anim_out_right);
    }
}