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

import com.google.android.material.textfield.TextInputLayout;

import java.io.InputStream;
import java.util.Objects;

import app.edu.app.R;
import app.edu.app.dao.LoaiHangDAO;
import app.edu.app.model.LoaiHang;
import app.edu.app.utils.ImageToByte;
import app.edu.app.utils.MyToast;

public class ThemLoaiActivity extends AppCompatActivity implements View.OnClickListener{
    private static final int PICK_IMAGE = 1;
    boolean pickImageSatus = false;

    ImageView ivBack, ivHinhAnh, ivPickImage;
    TextInputLayout tilTenLoai;
    Button btnAdd;
    LoaiHangDAO loaiHangDAO;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_them_loai);
        initView();

        loaiHangDAO = new LoaiHangDAO(this);

        ivBack.setOnClickListener(this);
        ivPickImage.setOnClickListener(this);
        btnAdd.setOnClickListener(this);

        initImagePicker();

    }

    private void initView() {
        ivBack = findViewById(R.id.ivBack);
        ivHinhAnh = findViewById(R.id.ivHinhAnh);
        ivPickImage = findViewById(R.id.ivPickImage);
        tilTenLoai = findViewById(R.id.tilTenLoaiHang);
        btnAdd = findViewById(R.id.btnAdd);
    }

    private void addProduct() {
        // Thêm loại hàng mới
        if (!pickImageSatus) {
            MyToast.error(ThemLoaiActivity.this, "Vui lòng chọn ảnh");
        } else {
            String tenLoai = Objects.requireNonNull(tilTenLoai.getEditText()).getText().toString().trim();
            if (tenLoai.isEmpty()) {
                MyToast.error(ThemLoaiActivity.this, "Vui lòng nhập tên loại");
            } else {
                LoaiHang loaiHang = new LoaiHang();
                loaiHang.setTenLoai(tenLoai);
                loaiHang.setHinhAnh(ImageToByte.imageViewToByte(getApplicationContext(), ivHinhAnh));
                if (loaiHangDAO.insertLoaiHang(loaiHang)) {
                    MyToast.successful(ThemLoaiActivity.this, "Thêm thành công");

                    tilTenLoai.getEditText().setText("");
                    ivHinhAnh.setImageResource(R.drawable.pick_image1);
                    pickImageSatus = false;
                } else {
                    MyToast.error(ThemLoaiActivity.this, "Thêm không thành công");
                }
            }
        }
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
        super.onActivityResult(requestCode, resultCode, data);
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
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View view) {
//        switch (view.getId()){
//            case R.id.ivBack:
//                onBackPressed();
//                break;
//            case R.id.ivPickImage:
//                pickImage();
//                break;
//            case R.id.btnAdd:
//                addProduct();
//                break;
//        }
        if (view.getId() == R.id.ivBack) {
            onBackPressed();
        } else if (view.getId() == R.id.ivPickImage) {
            pickImage();
        } else if (view.getId() == R.id.btnAdd) {
            addProduct();
        }
    }
}