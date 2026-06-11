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

public class ThemNhanVienActivity extends AppCompatActivity implements View.OnClickListener {
    ImageView ivBack;
    TextInputLayout tilMaNguoiDung, tilHoVaTen, tilNgaySinh, tilEmail, tilMatKhau;
    TextInputEditText tieNgaySinh;
    Button btnAdd;
    RadioGroup rdgGender;
    NguoiDungDAO nguoiDungDAO;
    public static final String MATCHES_EMAIL = "^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_them_thanh_vien);
        initView();
        nguoiDungDAO = new NguoiDungDAO(this);
        ivBack.setOnClickListener(this);
        btnAdd.setOnClickListener(this);
        tieNgaySinh.setOnClickListener(this);
    }

    private void initView() {
        ivBack = findViewById(R.id.ivBack);
        tilMaNguoiDung = findViewById(R.id.tilMaNguoiDung);
        tilHoVaTen = findViewById(R.id.tilHoVaTen);
        tilNgaySinh = findViewById(R.id.tilNgaySinh);
        tilEmail = findViewById(R.id.tilEmail);
        tilMatKhau = findViewById(R.id.tilMatKhau);
        tieNgaySinh = findViewById(R.id.tieNgaySinh);
        btnAdd = findViewById(R.id.btnAdd);
        rdgGender = findViewById(R.id.rdgGender);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View view) {
//        switch (view.getId()) {
//            case R.id.ivBack:
//                onBackPressed();
//                break;
//            case R.id.btnAdd:
//                addNguoiDung();
//                break;
//            case R.id.tieNgaySinh:
//                showDatePicker();
//                break;
//        }
        if (view.getId() == R.id.ivBack) {
            onBackPressed();
        } else if (view.getId() == R.id.btnAdd) {
            addNguoiDung();
        } else if (view.getId() == R.id.tieNgaySinh) {
            showDatePicker();
        }
    }

    private void addNguoiDung() {
        String maNguoiDung = getText(tilMaNguoiDung);
        String hoVaTen = getText(tilHoVaTen);
        String ngaySinh = getText(tilNgaySinh);
        String email = getText(tilEmail);
        String matKhau = getText(tilMatKhau);
        if (maNguoiDung.isEmpty() || hoVaTen.isEmpty() || ngaySinh.isEmpty() || email.isEmpty() || matKhau.isEmpty()) {
            MyToast.error(ThemNhanVienActivity.this, "Vui lòng nhập đầy đủ thông tin");
        } else {
            try {
                Date date = XDate.toDate(ngaySinh);
                if (!email.matches(MATCHES_EMAIL)) {
                    MyToast.error(ThemNhanVienActivity.this, "Nhập email sai định dạng");
                } else {
                    addStaff(maNguoiDung, hoVaTen, ngaySinh, email, matKhau);
                }
            } catch (ParseException e) {
                e.printStackTrace();
                MyToast.error(ThemNhanVienActivity.this, "Nhập ngày sinh sai định dạng");
            }
        }
    }

    private void addStaff(String maNguoiDung, String hoVaTen, String ngaySinh, String email, String matKhau) throws ParseException {
        // Thêm nhân viên
        NguoiDung nguoiDung = new NguoiDung();
        nguoiDung.setMaNguoiDung(maNguoiDung);
        nguoiDung.setHoVaTen(hoVaTen);
        nguoiDung.setHinhAnh(ImageToByte.drawableToByte(ThemNhanVienActivity.this, R.drawable.avatar_user_md));
        nguoiDung.setNgaySinh(XDate.toDate(ngaySinh));
        nguoiDung.setEmail(email);
        nguoiDung.setChucVu(NguoiDung.POSITION_STAFF);
        nguoiDung.setGioiTinh(getGender());
        nguoiDung.setMatKhau(matKhau);
        if (nguoiDungDAO.insertNguoiDung(nguoiDung)) {
            MyToast.successful(ThemNhanVienActivity.this, "Thêm thành công");
            clearText();
        } else {
            MyToast.error(ThemNhanVienActivity.this, "Tên đăng nhập tồn tại");
        }
    }

    private void clearText() {
        Objects.requireNonNull(tilMaNguoiDung.getEditText()).setText("");
        Objects.requireNonNull(tilHoVaTen.getEditText()).setText("");
        Objects.requireNonNull(tilNgaySinh.getEditText()).setText("");
        Objects.requireNonNull(tilEmail.getEditText()).setText("");
        Objects.requireNonNull(tilMatKhau.getEditText()).setText("");
    }

    @NonNull
    private String getText(TextInputLayout tilMaNguoiDung) {
        return Objects.requireNonNull(tilMaNguoiDung.getEditText()).getText().toString().trim();
    }

    private void showDatePicker() {
        // Show dialog chọn ngày
        Calendar calendar = Calendar.getInstance();
        int day = calendar.get(Calendar.DATE);
        int month = calendar.get(Calendar.MONTH);
        int year = calendar.get(Calendar.YEAR);
        DatePickerDialog datePickerDialog = new DatePickerDialog(ThemNhanVienActivity.this,
                R.style.MyDatePickerDialogTheme, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker datePicker, int y, int m, int d) {
                calendar.set(y, m, d);
                tieNgaySinh.setText(XDate.toStringDate(calendar.getTime()));
            }
        }, year, month, day);
        datePickerDialog.show();
    }

    private String getGender() {
        // Giới tính
        if (rdgGender.getCheckedRadioButtonId() == R.id.rdNam) {
            return NguoiDung.GENDER_MALE;
        }
        return NguoiDung.GENDER_FEMALE;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.anim_in_left, R.anim.anim_out_right);
    }
}