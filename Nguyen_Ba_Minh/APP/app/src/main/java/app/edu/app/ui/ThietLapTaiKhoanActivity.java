package app.edu.app.ui;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.google.android.material.textfield.TextInputLayout;
import com.squareup.picasso.Picasso;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;
import app.edu.app.R;
import app.edu.app.dao.NguoiDungDAO;
import app.edu.app.fragment.SettingFragment;
import app.edu.app.interfaces.OnUrlFetchedListener;
import app.edu.app.model.NguoiDung;
import app.edu.app.utils.ImageCache;
import app.edu.app.utils.MyToast;
import app.edu.app.utils.XDate;

public class ThietLapTaiKhoanActivity extends AppCompatActivity implements View.OnClickListener {

    ImageView ivBack, ivEditTen, ivEditNgaySinh, ivEditEmail, ivEditGioiTinh, ivEditChucVu;
    CircleImageView civHinhAnh;
    TextView tvMaNguoiDung, tvTenNguoiDung, tvNgaySinh, tvGioiTinh, tvEmail, tvChucVu;
    NguoiDungDAO nguoiDungDAO;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_thiet_lap_tai_khoan);
        initView();
        nguoiDungDAO = new NguoiDungDAO(this);
        getInfoNguoiDung();
        initOnclickIv();
    }


    private void initView() {
        ivBack = findViewById(R.id.ivBack);
        civHinhAnh = findViewById(R.id.civHinhAnh);
        tvMaNguoiDung = findViewById(R.id.tvMaNguoiDung);
        tvTenNguoiDung = findViewById(R.id.tvTenNguoiDung);
        tvNgaySinh = findViewById(R.id.tvNgaySinh);
        tvGioiTinh = findViewById(R.id.tvGioiTinh);
        tvEmail = findViewById(R.id.tvEmail);
        tvChucVu = findViewById(R.id.tvChucVu);
        ivEditTen = findViewById(R.id.ivEditHoVaTen);
        ivEditNgaySinh = findViewById(R.id.ivEditNgaySinh);
        ivEditEmail = findViewById(R.id.ivEditEmail);
        ivEditGioiTinh = findViewById(R.id.ivEditGioiTinh);
        ivEditChucVu = findViewById(R.id.ivEditChucVu);
    }

    private void initOnclickIv() {
        ivBack.setOnClickListener(this);
        ivEditTen.setOnClickListener(this);
        ivEditGioiTinh.setOnClickListener(this);
        ivEditNgaySinh.setOnClickListener(this);
        ivEditEmail.setOnClickListener(this);
        ivEditChucVu.setOnClickListener(this);
    }

    private NguoiDung getObjectNguoiDung() {
        Intent intent = getIntent();
        String maNguoiDung = intent.getStringExtra(SettingFragment.MA_NGUOIDUNG);
        return nguoiDungDAO.getByMaNguoiDung(maNguoiDung);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View view) {
//        switch (view.getId()) {
//            case R.id.ivBack:
//                onBackPressed();
//                break;
//            case R.id.ivEditHoVaTen:
//                showDialogEditTen();
//                break;
//            case R.id.ivEditNgaySinh:
//                showDialogEditNgaySinh();
//                break;
//            case R.id.ivEditGioiTinh:
//                showDialogEditGioiTinh();
//                break;
//            case R.id.ivEditEmail:
//                showDialogEditEmail();
//                break;
//            case R.id.ivEditChucVu:
//                showDialogEditChucVu();
//                break;
//        }
        if (view.getId() == R.id.ivBack) {
            onBackPressed();
        } else if (view.getId() == R.id.ivEditHoVaTen) {
            showDialogEditTen();
        } else if (view.getId() == R.id.ivEditNgaySinh) {
            showDialogEditNgaySinh();
        } else if (view.getId() == R.id.ivEditGioiTinh) {
            showDialogEditGioiTinh();
        } else if (view.getId() == R.id.ivEditEmail) {
            showDialogEditEmail();
        } else if (view.getId() == R.id.ivEditChucVu) {
            showDialogEditChucVu();
        }
    }

    @SuppressLint("SetTextI18n")
    private void getInfoNguoiDung() {
        NguoiDung nguoiDung = getObjectNguoiDung();
//        Bitmap bitmap = BitmapFactory.decodeByteArray(nguoiDung.getHinhAnh(), 0, nguoiDung.getHinhAnh().length);
//        civHinhAnh.setImageBitmap(bitmap);
        ImageCache.getUrlFromCache("nguoidung_" + nguoiDung.getMaNguoiDung(), new OnUrlFetchedListener() {
            @Override
            public void onUrlFetched(String url) {
                Picasso.get().load(url).into(civHinhAnh);
            }
        });

        tvMaNguoiDung.setText(nguoiDung.getMaNguoiDung());
        tvTenNguoiDung.setText(nguoiDung.getHoVaTen());
        tvNgaySinh.setText(XDate.toStringDate(nguoiDung.getNgaySinh()));
        if (nguoiDung.getGioiTinh().equals(NguoiDung.GENDER_FEMALE)) {
            tvGioiTinh.setText("Nữ");
        } else {
            tvGioiTinh.setText("Nam");
        }
        tvEmail.setText(nguoiDung.getEmail());
        tvChucVu.setText(nguoiDung.getChucVu());
    }

    private void showDialogEditTen() {
        NguoiDung nguoiDung = getObjectNguoiDung();
        View viewDialog = LayoutInflater.from(this).inflate(R.layout.layout_edit_ten_nd, null);
        TextInputLayout tilNgaySinh = viewDialog.findViewById(R.id.til);
        Objects.requireNonNull(tilNgaySinh.getEditText()).setText(nguoiDung.getHoVaTen());
        Button btnUpdate = viewDialog.findViewById(R.id.btnUpdate);
        TextView tvBoQua = viewDialog.findViewById(R.id.tvBoQua);
        Dialog dialog = new Dialog(this);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        dialog.setContentView(viewDialog);
        int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.90);
        int height = WindowManager.LayoutParams.WRAP_CONTENT;
        dialog.getWindow().setLayout(width, height);
        tvBoQua.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
        btnUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String hoVaTen = tilNgaySinh.getEditText().getText().toString().trim();
                if (hoVaTen.isEmpty()) {
                    MyToast.error(ThietLapTaiKhoanActivity.this, "Vui lòng không để trống");
                } else {
                    // Cập nhật lại họ và tên
                    nguoiDung.setHoVaTen(hoVaTen);
                    if (nguoiDungDAO.updateNguoiDung(nguoiDung)) {
                        MyToast.successful(ThietLapTaiKhoanActivity.this, "Cập nhật thành công");
                        dialog.dismiss();
                    }
                }
                getInfoNguoiDung();
            }
        });
        dialog.show();
    }

    private void showDialogEditEmail() {
        NguoiDung nguoiDung = getObjectNguoiDung();
        View viewDialog = LayoutInflater.from(this).inflate(R.layout.layout_edit_email_nd, null);
        TextInputLayout tilEmail = viewDialog.findViewById(R.id.til);
        Objects.requireNonNull(tilEmail.getEditText()).setText(nguoiDung.getEmail());
        Button btnUpdate = viewDialog.findViewById(R.id.btnUpdate);
        TextView tvBoQua = viewDialog.findViewById(R.id.tvBoQua);
        Dialog dialog = new Dialog(this);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        dialog.setContentView(viewDialog);
        int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.90);
        int height = WindowManager.LayoutParams.WRAP_CONTENT;
        dialog.getWindow().setLayout(width, height);
        tvBoQua.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
        btnUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email = tilEmail.getEditText().getText().toString().trim();
                if (email.isEmpty()) {
                    MyToast.error(ThietLapTaiKhoanActivity.this, "Vui lòng không để trống");
                } else {
                    // Cập nhật Email
                    nguoiDung.setEmail(email);
                    if (nguoiDungDAO.updateNguoiDung(nguoiDung)) {
                        MyToast.successful(ThietLapTaiKhoanActivity.this, "Cập nhật thành công");
                        dialog.dismiss();
                    }
                }
                getInfoNguoiDung();
            }
        });
        dialog.show();
    }

    private void showDialogEditNgaySinh() {
        NguoiDung nguoiDung = getObjectNguoiDung();
        View viewDialog = LayoutInflater.from(this).inflate(R.layout.layout_edit_ngay_sinh, null);
        TextInputLayout tilNgaySinh = viewDialog.findViewById(R.id.til);
        Objects.requireNonNull(tilNgaySinh.getEditText()).setText(XDate.toStringDate(nguoiDung.getNgaySinh()));
        Button btnUpdate = viewDialog.findViewById(R.id.btnUpdate);
        TextView tvBoQua = viewDialog.findViewById(R.id.tvBoQua);
        Dialog dialog = new Dialog(this);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        dialog.setContentView(viewDialog);
        int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.90);
        int height = WindowManager.LayoutParams.WRAP_CONTENT;
        dialog.getWindow().setLayout(width, height);
        tvBoQua.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
        btnUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String ngaySinh = tilNgaySinh.getEditText().getText().toString().trim();
                if (ngaySinh.isEmpty()) {
                    MyToast.error(ThietLapTaiKhoanActivity.this, "Vui lòng không để trống");
                } else {
                    try {
                        Date date = XDate.toDate(ngaySinh);
                        nguoiDung.setNgaySinh(date);
                        if (nguoiDungDAO.updateNguoiDung(nguoiDung)) {
                            MyToast.successful(ThietLapTaiKhoanActivity.this, "Cập nhật thành công");
                            dialog.dismiss();
                        }
                    } catch (ParseException e) {
                        e.printStackTrace();
                        MyToast.error(ThietLapTaiKhoanActivity.this, "Nhập ngày sinh sai dịnh dạng");
                    }
                }
                getInfoNguoiDung();
            }
        });

        // Hiển thị Dialog chọn ngày
        tilNgaySinh.getEditText().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(nguoiDung.getNgaySinh());
                int date = calendar.get(Calendar.DATE);
                int month = calendar.get(Calendar.MONTH);
                int year = calendar.get(Calendar.YEAR);
                DatePickerDialog datePickerDialog = new DatePickerDialog(ThietLapTaiKhoanActivity.this, R.style.MyDatePickerDialogTheme,new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker datePicker, int i, int i1, int i2) {
                        calendar.set(i, i1, i2);
                        tilNgaySinh.getEditText().setText(XDate.toStringDate(calendar.getTime()));
                    }
                }, year, month, date);
                datePickerDialog.show();
            }
        });
        dialog.show();
    }

    private void showDialogEditGioiTinh() {
        NguoiDung nguoiDung = getObjectNguoiDung();
        View viewDialog = LayoutInflater.from(this).inflate(R.layout.layout_edit_gioi_tinh_nd, null);
        RadioGroup radioGroup = viewDialog.findViewById(R.id.grGender);
        RadioButton rdNam = viewDialog.findViewById(R.id.rbNam);
        RadioButton rdNu = viewDialog.findViewById(R.id.rbNu);
        if (nguoiDung.getGioiTinh().equals(NguoiDung.GENDER_MALE)) {
            rdNam.setChecked(true);
        } else {
            rdNu.setChecked(true);
        }
        Button btnUpdate = viewDialog.findViewById(R.id.btnUpdate);
        TextView tvBoQua = viewDialog.findViewById(R.id.tvBoQua);
        Dialog dialog = new Dialog(this);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        dialog.setContentView(viewDialog);
        int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.90);
        int height = WindowManager.LayoutParams.WRAP_CONTENT;
        dialog.getWindow().setLayout(width, height);
        tvBoQua.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
        btnUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int rbCheck = radioGroup.getCheckedRadioButtonId();
                if (rbCheck == R.id.rbNam) {
                    nguoiDung.setGioiTinh(NguoiDung.GENDER_MALE);
                } else {
                    nguoiDung.setGioiTinh(NguoiDung.GENDER_FEMALE);
                }
                if (nguoiDungDAO.updateNguoiDung(nguoiDung)) {
                    MyToast.successful(ThietLapTaiKhoanActivity.this, "Cập nhật thành công");
                    dialog.dismiss();
                }
                getInfoNguoiDung();
            }
        });
        dialog.show();
    }

    private void showDialogEditChucVu() {
        NguoiDung nguoiDung = getObjectNguoiDung();
        View viewDialog = LayoutInflater.from(this).inflate(R.layout.layout_edit_chuc_vu, null);
        RadioGroup radioGroup = viewDialog.findViewById(R.id.grChucVu);
        RadioButton rdAdmin = viewDialog.findViewById(R.id.rbAdmin);
        RadioButton rdNhanVien = viewDialog.findViewById(R.id.rbNhanVien);
        if (nguoiDung.getChucVu().equals(NguoiDung.POSITION_ADMIN)) {
            rdAdmin.setChecked(true);
        } else {
            rdNhanVien.setChecked(true);
        }
        Button btnUpdate = viewDialog.findViewById(R.id.btnUpdate);
        TextView tvBoQua = viewDialog.findViewById(R.id.tvBoQua);
        Dialog dialog = new Dialog(this);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        dialog.setContentView(viewDialog);
        int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.90);
        int height = WindowManager.LayoutParams.WRAP_CONTENT;
        dialog.getWindow().setLayout(width, height);
        tvBoQua.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
        btnUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int rbCheck = radioGroup.getCheckedRadioButtonId();
                if (rbCheck == R.id.rbAdmin) {
                    nguoiDung.setChucVu(NguoiDung.POSITION_ADMIN);
                } else {
                    nguoiDung.setChucVu(NguoiDung.POSITION_STAFF);
                }
                if (nguoiDungDAO.updateNguoiDung(nguoiDung)) {
                    MyToast.successful(ThietLapTaiKhoanActivity.this, "Cập nhật thành công");
                    dialog.dismiss();
                }
                getInfoNguoiDung();
            }
        });
        dialog.show();
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.anim_in_left, R.anim.anim_out_right);
    }
}