package app.edu.app.ui;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;

import com.google.android.material.textfield.TextInputLayout;

import java.io.InputStream;
import java.util.Objects;

import app.edu.app.R;
import app.edu.app.adapter.SpinnerAdapterLoaiHangMain;
import app.edu.app.dao.HangHoaDAO;
import app.edu.app.dao.LoaiHangDAO;
import app.edu.app.model.HangHoa;
import app.edu.app.model.LoaiHang;
import app.edu.app.utils.ImageToByte;
import app.edu.app.utils.MyToast;

public class ThemHangHoaActivity extends AppCompatActivity implements View.OnClickListener {
    private static final int PICK_IMAGE = 1;
    boolean pickImageSatus = false;
    ImageView ivBack, ivHinhAnh, pickHinhAnh;
    Spinner spinner;
    TextInputLayout tilTenHangHoa, tilGiaTien;
    Button btnAdd;
    LoaiHangDAO loaiHangDAO;
    HangHoaDAO hangHoaDAO;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_them_hang_hoa);
        initView();

        loaiHangDAO = new LoaiHangDAO(this);
        hangHoaDAO = new HangHoaDAO(this);

        ivBack.setOnClickListener(this);
        pickHinhAnh.setOnClickListener(this);
        btnAdd.setOnClickListener(this);

        SpinnerAdapterLoaiHangMain adapterLoaiHangMain = new SpinnerAdapterLoaiHangMain(ThemHangHoaActivity.this, loaiHangDAO.getAll());
        spinner.setAdapter(adapterLoaiHangMain);

        initImagePicker();
    }

    private void initView() {
        ivBack = findViewById(R.id.ivBack);
        ivHinhAnh = findViewById(R.id.ivHinhAnh);
        pickHinhAnh = findViewById(R.id.ivPickImage);
        spinner = findViewById(R.id.spLoaiHang);
        tilTenHangHoa = findViewById(R.id.tilTenHangHoa);
        tilGiaTien = findViewById(R.id.tilGiaTien);
        btnAdd = findViewById(R.id.btnAdd);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View view) {
//        switch (view.getId()) {
//            case R.id.ivBack:
//                onBackPressed();
//                break;
//            case R.id.ivPickImage:
//                pickImage();
//                break;
//            case R.id.btnAdd:
//                if(!pickImageSatus){
//                    MyToast.error(ThemHangHoaActivity.this, "Vui lòng chọn hình ảnh");
//                }else {
//                    addHangHoa();
//                }
//                break;
//        }
        if (view.getId() == R.id.ivBack) {
            onBackPressed();
        } else if (view.getId() == R.id.ivPickImage) {
            pickImage();
        } else if (view.getId() == R.id.btnAdd) {
            if (!pickImageSatus) {
                MyToast.error(ThemHangHoaActivity.this, "Vui lòng chọn hình ảnh");
            } else {
                addHangHoa();
            }
        }
    }

    private void addHangHoa() {
        String tenHangHoa = Objects.requireNonNull(tilTenHangHoa.getEditText()).getText().toString();
        String giaTien = Objects.requireNonNull(tilGiaTien.getEditText()).getText().toString();
        if(tenHangHoa.isEmpty() || giaTien.isEmpty()){
            MyToast.error(ThemHangHoaActivity.this, "Vui lòng nhập tên thức uống");
        }else {
            LoaiHang loaiHang = (LoaiHang) spinner.getSelectedItem();
            HangHoa hangHoa = new HangHoa();
            hangHoa.setMaLoai(loaiHang.getMaLoai());
            hangHoa.setHinhAnh(ImageToByte.imageViewToByte(ThemHangHoaActivity.this,ivHinhAnh));
            hangHoa.setTenHangHoa(tenHangHoa);
            hangHoa.setGiaTien(Integer.parseInt(giaTien));
            hangHoa.setTrangThai(HangHoa.STATUS_STILL);
            if(hangHoaDAO.insertHangHoa(hangHoa)){
                MyToast.successful(ThemHangHoaActivity.this, "Thêm thành công");
                // reset data
                ivHinhAnh.setImageResource(R.drawable.pick_image1);
                pickImageSatus = false;
                clearText();
            }else {
                MyToast.error(ThemHangHoaActivity.this, "Thêm không thành công");
            }
        }
    }

    private void clearText() {
        Objects.requireNonNull(tilTenHangHoa.getEditText()).setText("");
        Objects.requireNonNull(tilGiaTien.getEditText()).setText("");
    }

    private void pickImage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Sử dụng ActivityResultLauncher cho Android 11+
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePickerLauncher.launch(intent);
        } else {
            // Sử dụng phương thức cũ cho Android 10-
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE);
        }
    }

    private ActivityResultLauncher<Intent> imagePickerLauncher;

    // Khởi tạo trong onCreate() hoặc trong constructor của Fragment
    private void initImagePicker() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        // Xử lý kết quả tại đây - tương tự như trong onActivityResult cũ
                        Uri imageUri = result.getData().getData();
                        try {
                            InputStream stream = getApplicationContext().getContentResolver().openInputStream(imageUri);
                            Bitmap bitmap = BitmapFactory.decodeStream(stream);
                            pickImageSatus = true;
                            ivHinhAnh.setImageBitmap(bitmap);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.anim_in_left, R.anim.anim_out_right);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            try {
                InputStream stream = getApplicationContext().getContentResolver().openInputStream(uri);
                Bitmap bitmap = BitmapFactory.decodeStream(stream);
                pickImageSatus = true;
                ivHinhAnh.setImageBitmap(bitmap);
            } catch (Exception e) {
                e.getMessage();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}