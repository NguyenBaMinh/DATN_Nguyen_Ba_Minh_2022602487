package app.edu.app.ui;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.squareup.picasso.Picasso;

import java.io.InputStream;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;
import app.edu.app.R;
import app.edu.app.dao.NguoiDungDAO;
import app.edu.app.interfaces.OnUrlFetchedListener;
import app.edu.app.model.NguoiDung;
import app.edu.app.utils.ImageCache;
import app.edu.app.utils.ImageToByte;
import app.edu.app.utils.MyToast;
import app.edu.app.utils.XDate;

public class CapNhatNhanVienActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int PICK_IMAGE = 1;
    public static final String MATCHES_EMAIL = "^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$";
    NguoiDungDAO nguoiDungDAO;
    ImageView ivBack, pickHinhAnh;
    CircleImageView civHinhAnh;
    TextInputLayout tilMaNguoiDung, tilHoVaTen, tilNgaySinh, tilEmail, tilMatKhau;
    TextInputEditText tieNgaySinh;
    RadioGroup rdgGender;
    RadioButton rdNam, rdNu;
    Button btnUpdate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cap_nhat_nhan_vien);
        initView();
        nguoiDungDAO = new NguoiDungDAO(this);
        ivBack.setOnClickListener(this);
        tieNgaySinh.setOnClickListener(this);
        pickHinhAnh.setOnClickListener(this);
        btnUpdate.setOnClickListener(this);
        fillData();
    }

    private void initView() {
        ivBack = findViewById(R.id.ivBack);
        civHinhAnh = findViewById(R.id.civHinhAnh);
        tilMaNguoiDung = findViewById(R.id.tilMaNguoiDung);
        tilHoVaTen = findViewById(R.id.tilHoVaTen);
        tilNgaySinh = findViewById(R.id.tilNgaySinh);
        tilEmail = findViewById(R.id.tilEmail);
        tilMatKhau = findViewById(R.id.tilMatKhau);
        tieNgaySinh = findViewById(R.id.tieNgaySinh);
        rdgGender = findViewById(R.id.rdgGender);
        btnUpdate = findViewById(R.id.btnUpdate);
        pickHinhAnh = findViewById(R.id.pickHinhAnh);
        rdNam = findViewById(R.id.rdNam);
        rdNu = findViewById(R.id.rdNu);
    }

    private void fillData() {
        NguoiDung nguoiDung = getObjectNguoiDung();
        ImageCache.getUrlFromCache("nguoidung_" + nguoiDung.getMaNguoiDung(), new OnUrlFetchedListener() {
            @Override
            public void onUrlFetched(String url) {
                Picasso.get().load(url).into(civHinhAnh);
            }
        });
        setText(tilMaNguoiDung, String.valueOf(nguoiDung.getMaNguoiDung()));
        setText(tilHoVaTen, nguoiDung.getHoVaTen());
        setText(tilNgaySinh, XDate.toStringDate(nguoiDung.getNgaySinh()));
        if (nguoiDung.getGioiTinh().equals(NguoiDung.GENDER_MALE)) {
            rdNam.setChecked(true);
        } else {
            rdNu.setChecked(true);
        }
        setText(tilEmail, nguoiDung.getEmail());
        setText(tilMatKhau, nguoiDung.getMatKhau());
    }

    private NguoiDung getObjectNguoiDung() {
        Intent intent = getIntent();
        String maNguoiDung = intent.getStringExtra(NhanVienActivity.MA_NGUOI_DUNG);
        return nguoiDungDAO.getByMaNguoiDung(maNguoiDung);
    }

    private void updateNhanVien() {
        String maNguoiDung = getText(tilMaNguoiDung);
        String hoVaTen = getText(tilHoVaTen);
        String ngaySinh = getText(tilNgaySinh);
        String email = getText(tilEmail);
        String matKhau = getText(tilMatKhau);
        if (maNguoiDung.isEmpty() || hoVaTen.isEmpty() || ngaySinh.isEmpty() || email.isEmpty() || matKhau.isEmpty()) {
            MyToast.error(CapNhatNhanVienActivity.this, "Vui lòng nhập đầy đủ thông tin");
        } else {
            try {
                Date date = XDate.toDate(ngaySinh);
                if (!email.matches(MATCHES_EMAIL)) {
                    MyToast.error(CapNhatNhanVienActivity.this, "Nhập email sai định dạng");
                } else {
                    // Cập nhật nhân viên
                    update(maNguoiDung, hoVaTen, ngaySinh, email, matKhau);
                }
            } catch (ParseException e) {
                e.printStackTrace();
                MyToast.error(CapNhatNhanVienActivity.this, "Nhập ngày sinh sai định dạng");
            }
        }

    }

    private void update(String maNguoiDung, String hoVaTen, String ngaySinh, String email, String matKhau) throws ParseException {
        NguoiDung nguoiDung = getObjectNguoiDung();
        nguoiDung.setMaNguoiDung(maNguoiDung);
        nguoiDung.setHoVaTen(hoVaTen);
        nguoiDung.setHinhAnh(ImageToByte.circleImageViewToByte(CapNhatNhanVienActivity.this, civHinhAnh));
        nguoiDung.setNgaySinh(XDate.toDate(ngaySinh));
        nguoiDung.setEmail(email);
        nguoiDung.setChucVu(NguoiDung.POSITION_STAFF);
        nguoiDung.setGioiTinh(getGender());
        nguoiDung.setMatKhau(matKhau);
        if (nguoiDungDAO.updateNguoiDung(nguoiDung)) {
            MyToast.successful(CapNhatNhanVienActivity.this, "Cập nhật thành công");
        } else {
            MyToast.successful(CapNhatNhanVienActivity.this, "Cập nhật không thành công");
        }
    }

    @NonNull
    private String getText(TextInputLayout tilMaNguoiDung) {
        return Objects.requireNonNull(tilMaNguoiDung.getEditText()).getText().toString().trim();
    }

    private void setText(TextInputLayout til, String s) {
        Objects.requireNonNull(til.getEditText()).setText(s);
    }

    private String getGender() {
        // Giới tính
        if (rdgGender.getCheckedRadioButtonId() == R.id.rdNam) {
            return NguoiDung.GENDER_MALE;
        }
        return NguoiDung.GENDER_FEMALE;
    }

    private void showDateTimePicker() throws ParseException {
        NguoiDung nguoiDung = getObjectNguoiDung();
        Objects.requireNonNull(tilNgaySinh.getEditText()).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(nguoiDung.getNgaySinh());
                int date = calendar.get(Calendar.DATE);
                int month = calendar.get(Calendar.MONTH);
                int year = calendar.get(Calendar.YEAR);
                DatePickerDialog datePickerDialog = new DatePickerDialog(CapNhatNhanVienActivity.this, R.style.MyDatePickerDialogTheme, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker datePicker, int i, int i1, int i2) {
                        calendar.set(i, i1, i2);
                        setText(tilNgaySinh, XDate.toStringDate(calendar.getTime()));
                    }
                }, year, month, date);
                datePickerDialog.show();
            }
        });
    }

    private void pickImage() {
        // Chọn hình trong Phone
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED) {
            // cấp quyền cho ứng dụng
            ActivityCompat.requestPermissions(CapNhatNhanVienActivity.this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    1);
        } else {

            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            try {
                InputStream stream = getApplicationContext().getContentResolver().openInputStream(uri);
                Bitmap bitmap = BitmapFactory.decodeStream(stream);
                civHinhAnh.setImageBitmap(bitmap);
            } catch (Exception e) {
                e.getMessage();
            }
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.anim_in_left, R.anim.anim_out_right);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View view) {
//        switch (view.getId()) {
//            case R.id.ivBack:
//                onBackPressed();
//                break;
//            case R.id.btnUpdate:
//                updateNhanVien();
//                break;
//            case R.id.tieNgaySinh:
//                try {
//                    showDateTimePicker();
//                } catch (ParseException e) {
//                    e.printStackTrace();
//                }
//                break;
//            case R.id.pickHinhAnh:
//                pickImage();
//                break;
//        }

        if (view.getId() == R.id.ivBack) {
            onBackPressed();
        } else if (view.getId() == R.id.btnUpdate) {
            updateNhanVien();
        } else if (view.getId() == R.id.tieNgaySinh) {
            try {
                showDateTimePicker();
            } catch (ParseException e) {
                e.printStackTrace();
            }
        } else if (view.getId() == R.id.pickHinhAnh) {
            pickImage();
        }
    }
}