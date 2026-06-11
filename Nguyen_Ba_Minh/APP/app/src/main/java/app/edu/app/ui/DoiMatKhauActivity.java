package app.edu.app.ui;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.google.android.material.textfield.TextInputLayout;

import java.util.Calendar;
import java.util.Objects;

import app.edu.app.R;
import app.edu.app.dao.NguoiDungDAO;
import app.edu.app.dao.ThongBaoDAO;
import app.edu.app.fragment.SettingFragment;
import app.edu.app.model.NguoiDung;
import app.edu.app.model.ThongBao;
import app.edu.app.utils.MyToast;

public class DoiMatKhauActivity extends AppCompatActivity {
    ImageView ivBack;
    TextInputLayout tilMatKhauCu, tilMatKhauMoi, tilNhapLaiMatKhau;
    Button btnUpdate;
    NguoiDungDAO nguoiDungDAO;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doi_mat_khau);
        inniView();
        nguoiDungDAO = new NguoiDungDAO(this);
        ivBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
        btnUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updatePasword();
            }
        });

    }

    private void inniView() {
        ivBack = findViewById(R.id.ivBack);
        tilMatKhauCu = findViewById(R.id.tilMatKhauCu);
        tilMatKhauMoi = findViewById(R.id.tilMatKhauMoi);
        tilNhapLaiMatKhau = findViewById(R.id.tilNhapLaiMatKhau);
        btnUpdate = findViewById(R.id.btnUpdate);
    }

    @NonNull
    private String getText(TextInputLayout til) {
        return Objects.requireNonNull(til.getEditText()).getText().toString();
    }

    private void clearText() {
        Objects.requireNonNull(tilMatKhauCu.getEditText()).setText("");
        Objects.requireNonNull(tilMatKhauMoi.getEditText()).setText("");
        Objects.requireNonNull(tilNhapLaiMatKhau.getEditText()).setText("");
    }

    private String getMaNguoiDung() {
        Intent intent = getIntent();
        return intent.getStringExtra(SettingFragment.MA_NGUOIDUNG);
    }

    private void updatePasword() {
        // Lấy mã người dùng
        String maNguoiDung = getMaNguoiDung();
        // Lấy người dùng theo mã
        NguoiDung nguoiDung = nguoiDungDAO.getByMaNguoiDung(maNguoiDung);
        // Lấy dữ liệu của TextInputLayout
        String matKhauCu = getText(tilMatKhauCu);
        String matKhauMoi = getText(tilMatKhauMoi);
        String nhapLaimatKhau = getText(tilNhapLaiMatKhau);
        // Kiểm tra dữ liệu
        if (!matKhauCu.isEmpty() && !matKhauMoi.isEmpty() && !nhapLaimatKhau.isEmpty()) {
            // Nếu mật khẩu cũ, mật khẩu mới, mật khẩu nhập lại khác 0
            if (!matKhauCu.equals(nguoiDung.getMatKhau())) {
                // Kiểm tra mật khẩu nhập vào có trùng với mật khẩu cũ ?
                MyToast.error(DoiMatKhauActivity.this, "Mật khẩu cũ không chính xác");
            }
            if (!matKhauMoi.equals(nhapLaimatKhau)) {
                // Kiểm tra mật khẩu nhập lại có khớp với mật khẩu mới
                MyToast.error(DoiMatKhauActivity.this, "Mật khẩu mới không khớp");
            }
            if (matKhauCu.equals(nguoiDung.getMatKhau()) && matKhauMoi.equals(nhapLaimatKhau)) {
                /*
                Mật khẩu nhập vào bằng với mật khẩu cũ
                mật khẩu mơi khớp với mật khẩu nhập lại
                */
                // Gán lại mật khẩu mới
                nguoiDung.setMatKhau(matKhauMoi);
                if (nguoiDungDAO.updateNguoiDung(nguoiDung)) {
                    // Cập nhật mật khẩu
                    MyToast.successful(DoiMatKhauActivity.this, "Đổi mật khẩu thành công");
                    ThemThongBaoMoi();
                    clearText();
                    // Khai báo buider
                    AlertDialog.Builder builder = new AlertDialog.Builder(DoiMatKhauActivity.this, R.style.AlertDialogTheme);
                    // Gán thống báo
                    builder.setMessage("Quay lại màng hình đăng nhập?");
                    // Sự kiện đồng ý chuyển quan màng hình Đăng nhập
                    builder.setPositiveButton("Có", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            startActivity(new Intent(DoiMatKhauActivity.this, SignInActivity.class));

                        }
                    });
                    // Sự kiện huỷ
                    builder.setNegativeButton("Tiếp tục sử dụng", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            onBackPressed();
                        }
                    });
                    // Khởi tạo dialog và hiển thị
                    AlertDialog dialog = builder.create();
                    dialog.show();
                } else {
                    MyToast.error(DoiMatKhauActivity.this, "Đổi mật khẩu không thành công");
                }
            }
        } else {
            MyToast.error(DoiMatKhauActivity.this, "Vui lòng điền đầy đủ thông tin");
        }
    }

    private void ThemThongBaoMoi() {
        // Tạo thông báo cập nhật mật khẩu
        ThongBao thongBao = new ThongBao();
        thongBao.setNoiDung("Cập nhật mật khẩu thành công");
        thongBao.setTrangThai(ThongBao.STATUS_CHUA_XEM);
        Calendar calendar = Calendar.getInstance();
        thongBao.setNgayThongBao(calendar.getTime());
        ThongBaoDAO thongBaoDAO = new ThongBaoDAO(DoiMatKhauActivity.this);
        thongBaoDAO.insertThongBao(thongBao);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.anim_in_left, R.anim.anim_out_right);
    }
}