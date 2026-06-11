package app.edu.app.ui;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;

import app.edu.app.R;
import app.edu.app.dao.NguoiDungDAO;
import app.edu.app.model.NguoiDung;
import app.edu.app.utils.ImageToByte;
import app.edu.app.utils.MyToast;
import app.edu.app.utils.XDate;

public class SignUpActivity extends AppCompatActivity implements View.OnClickListener {
    ImageView ivBack;
    TextInputLayout tilMaNguoiDung, tilHoVaTen, tilNgaySinh, tilEmail, tilMatKhau;
    TextInputEditText tieNgaySinh;
    RadioGroup rdgGender, rdgPosition;
    Button btnSignUp;
    TextView tvSignIn;
    NguoiDungDAO nguoiDungDAO;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);
        initView();

        nguoiDungDAO = new NguoiDungDAO(this);

        ivBack.setOnClickListener(this);
        tvSignIn.setOnClickListener(this);
        tieNgaySinh.setOnClickListener(this);
        btnSignUp.setOnClickListener(this);
    }

    private void initView() {
        ivBack = findViewById(R.id.ivBack);
        tilMaNguoiDung = findViewById(R.id.tilMaNguoiDung);
        tilHoVaTen = findViewById(R.id.tilHoVaTen);
        tilNgaySinh = findViewById(R.id.tilNgaySinh);
        tilEmail = findViewById(R.id.tilEmail);
        tilMatKhau = findViewById(R.id.tilMatKhau);
        tieNgaySinh = findViewById(R.id.tieNgaySinh);
        rdgGender = findViewById(R.id.rdgGender);
        btnSignUp = findViewById(R.id.btnSignUp);
        tvSignIn = findViewById(R.id.tvSignIn);
    }

    private void showDateDialog() {
        Calendar calendar = Calendar.getInstance();
        // Lấy ngày, tháng, năm hiện tại
        int date = calendar.get(Calendar.DATE);
        int month = calendar.get(Calendar.MONTH);
        int year = calendar.get(Calendar.YEAR);
        // Hiển thị Dialog
        DatePickerDialog datePickerDialog = new DatePickerDialog(SignUpActivity.this,R.style.MyDatePickerDialogTheme, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker datePicker, int y, int m, int d) {
                calendar.set(y, m, d);
                tieNgaySinh.setText(XDate.toStringDate(calendar.getTime()));
            }
        }, year, month, date);

        datePickerDialog.show();
    }

    private void registerAccount() {
        String maNguoiDung = getText(tilMaNguoiDung);
        String hoTen = getText(tilHoVaTen);
        String ngaySinhh = getText(tilNgaySinh);
        String email = getText(tilEmail);
        String matKhau = getText(tilMatKhau);

        // Xử lý đăng ký
        if (maNguoiDung.isEmpty() || hoTen.isEmpty() || ngaySinhh.isEmpty() || email.isEmpty() || matKhau.isEmpty()) {
            MyToast.error(SignUpActivity.this, "Vui lòng nhập đẩy đủ thông tin");
        } else {
            if(isNgaySinh(ngaySinhh) && isEmail(email)){
                // Tạo Người Dùng mới
                NguoiDung nguoiDung = new NguoiDung();
                nguoiDung.setMaNguoiDung(maNguoiDung);
                nguoiDung.setHoVaTen(hoTen);
                nguoiDung.setHinhAnh(ImageToByte.drawableToByte(SignUpActivity.this, R.drawable.avatar_user_md));
                try {
                    nguoiDung.setNgaySinh(XDate.toDate(ngaySinhh));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                nguoiDung.setEmail(email);
                nguoiDung.setChucVu(getPosition());
                nguoiDung.setGioiTinh(getGender());
                nguoiDung.setMatKhau(matKhau);
                // Thêm Người Dùng
                if (nguoiDungDAO.insertNguoiDung(nguoiDung)) {
                    MyToast.successful(SignUpActivity.this, "Đăng ký thành công");
                    clearText();
                } else {
                    MyToast.error(SignUpActivity.this, "Tài khoản này hoặc email đã tồn tại");
                }
            }
        }
    }

    private boolean isEmail(String email) {
        // Kiểm tra định dạng Email
        if (!email.matches(NguoiDung.MATCHES_EMAIL)) {
            MyToast.error(SignUpActivity.this, "Nhập email sai định dạng");
            return false;
        }
        return true;
    }

    private boolean isNgaySinh(String ngaySinhh) {
        // Kiểm tra định dạng Ngày Sinh
        try {
            Date date = XDate.toDate(ngaySinhh);
            return true;
        } catch (ParseException e) {
            e.printStackTrace();
            MyToast.error(SignUpActivity.this, "Nhập ngày sinh sai định dạng");
            return false;
        }
    }

    private String getGender() {
        // Lấy giới tính chọn từ radio button
        if (rdgGender.getCheckedRadioButtonId() == R.id.rdNam) {
            return NguoiDung.GENDER_MALE;
        }
        return NguoiDung.GENDER_FEMALE;
    }

    private String getPosition() {
        return NguoiDung.POSITION_CUSTOMER;
    }


    private void clearText() {
        Objects.requireNonNull(tilMaNguoiDung.getEditText()).setText("");
        Objects.requireNonNull(tilHoVaTen.getEditText()).setText("");
        Objects.requireNonNull(tilNgaySinh.getEditText()).setText("");
        Objects.requireNonNull(tilEmail.getEditText()).setText("");
        Objects.requireNonNull(tilMatKhau.getEditText()).setText("");
    }

    @NonNull
    private String getText(TextInputLayout textInputLayout) {
        return Objects.requireNonNull(textInputLayout.getEditText()).getText().toString();
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View view) {
//        switch (view.getId()) {
//            case R.id.ivBack:
//            case R.id.tvSignIn:
//                onBackPressed();
//                break;
//            case R.id.tieNgaySinh:
//                showDateDialog();
//                break;
//            case R.id.btnSignUp:
//                registerAccount();
//                break;
//        }
        if (view.getId() == R.id.ivBack || view.getId() == R.id.tvSignIn) {
            onBackPressed();
        } else if (view.getId() == R.id.tieNgaySinh) {
            showDateDialog();
        } else if (view.getId() == R.id.btnSignUp) {
            registerAccount();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.anim_in_left, R.anim.anim_out_right);
    }
}