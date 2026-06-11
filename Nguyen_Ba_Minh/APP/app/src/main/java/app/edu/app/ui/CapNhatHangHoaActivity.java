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
import android.widget.RadioButton;
import android.widget.Spinner;

import com.google.android.material.textfield.TextInputLayout;
import com.squareup.picasso.Picasso;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Objects;

import app.edu.app.R;
import app.edu.app.adapter.SpinnerAdapterLoaiHangMain;
import app.edu.app.dao.HangHoaDAO;
import app.edu.app.dao.LoaiHangDAO;
import app.edu.app.interfaces.OnUrlFetchedListener;
import app.edu.app.model.HangHoa;
import app.edu.app.model.LoaiHang;
import app.edu.app.utils.ImageCache;
import app.edu.app.utils.ImageToByte;
import app.edu.app.utils.MyToast;

public class CapNhatHangHoaActivity extends AppCompatActivity implements View.OnClickListener {
    private static final int PICK_IMAGE = 1;
    ImageView ivBack, ivHinhAnh, ivPickImage;
    TextInputLayout tilMaHangHoa, tilTenHangHoa, tilGiaTien;
    Spinner spLoaiHang;
    Button btnUpdate;
    RadioButton rdConHang, rdHetHang;
    HangHoaDAO hangHoaDAO;
    LoaiHangDAO loaiHangDAO;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cap_nhat_hang_hoa);
        initView();


        hangHoaDAO = new HangHoaDAO(this);
        loaiHangDAO = new LoaiHangDAO(this);

        ivBack.setOnClickListener(this);
        ivPickImage.setOnClickListener(this);
        btnUpdate.setOnClickListener(this);

        fillSpinner();
        fillActivity();
        initImagePicker();
    }


    private void initView() {
        ivBack = findViewById(R.id.ivBack);
        ivHinhAnh = findViewById(R.id.ivHinhAnh);
        ivPickImage = findViewById(R.id.ivPickImage);
        tilMaHangHoa = findViewById(R.id.tilMaHangHoa);
        tilTenHangHoa = findViewById(R.id.tilTenHangHoa);
        tilGiaTien = findViewById(R.id.tilGiaTien);
        spLoaiHang = findViewById(R.id.spLoaiHang);
        btnUpdate = findViewById(R.id.btnUpdate);
        rdConHang = findViewById(R.id.rdConHang);
        rdHetHang = findViewById(R.id.rdHetHang);
    }

    private void fillActivity() {
        HangHoa hangHoa = getHangHoa();
        ImageCache.getUrlFromCache("" + hangHoa.getMaHangHoa(), new OnUrlFetchedListener() {
            @Override
            public void onUrlFetched(String url) {
                Picasso.get().load(url).into(ivHinhAnh);
            }
        });
        Objects.requireNonNull(tilMaHangHoa.getEditText()).setText(String.valueOf(hangHoa.getMaHangHoa()));
        Objects.requireNonNull(tilTenHangHoa.getEditText()).setText(hangHoa.getTenHangHoa());
        Objects.requireNonNull(tilGiaTien.getEditText()).setText(String.valueOf(hangHoa.getGiaTien()));
        if (hangHoa.getTrangThai() == HangHoa.STATUS_STILL) {
            rdConHang.setChecked(true);
        } else {
            rdHetHang.setChecked(true);
        }
        ArrayList<LoaiHang> arrayList = loaiHangDAO.getAll();
        for (int i = 0; i < arrayList.size(); i++){
            if(arrayList.get(i).getMaLoai() == hangHoa.getMaLoai()){
                spLoaiHang.setSelection(i);
            }
        }
    }

    private HangHoa getHangHoa() {
        Intent intent = getIntent();
        return hangHoaDAO.getByMaHangHoa(intent.getStringExtra(ThucUongActivity.MA_HANG_HOA));
    }

    private void fillSpinner() {
        SpinnerAdapterLoaiHangMain adapterLoaiHangMain = new SpinnerAdapterLoaiHangMain(this, loaiHangDAO.getAll());
        spLoaiHang.setAdapter(adapterLoaiHangMain);
    }

    private int getTrangThaiUpdate() {
        // Lây trạng thái
        if (rdHetHang.isChecked()) {
            return HangHoa.STATUS_OVER;
        }
        return HangHoa.STATUS_STILL;
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
//            case R.id.btnUpdate:
//                updateHangHoa();
//                break;
//        }
        if (view.getId() == R.id.ivBack) {
            onBackPressed();
        } else if (view.getId() == R.id.ivPickImage) {
            pickImage();
        } else if (view.getId() == R.id.btnUpdate) {
            updateHangHoa();
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

                            ivHinhAnh.setImageBitmap(bitmap);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
    }

    private void updateHangHoa() {
        // cập nhật hàng hoá
        HangHoa hangHoa = getHangHoa();
        String tenHangHoa = Objects.requireNonNull(tilTenHangHoa.getEditText()).getText().toString();
        String giaTien = Objects.requireNonNull(tilGiaTien.getEditText()).getText().toString();
        if (tenHangHoa.isEmpty() || giaTien.isEmpty()) {
            MyToast.error(CapNhatHangHoaActivity.this, "Vui lòng nhập đầy đủ thông tin");
        } else {
            hangHoa.setTenHangHoa(tenHangHoa);
            hangHoa.setGiaTien(Integer.parseInt(giaTien));
            hangHoa.setHinhAnh(ImageToByte.imageViewToByte(this, ivHinhAnh));
            hangHoa.setTrangThai(getTrangThaiUpdate());
            LoaiHang loaiHang = (LoaiHang) spLoaiHang.getSelectedItem();
            hangHoa.setMaLoai(loaiHang.getMaLoai());
            if (hangHoaDAO.updateHangHoa(hangHoa)) {
                MyToast.successful(CapNhatHangHoaActivity.this, "Cập nhật thành công");
            } else {
                MyToast.error(CapNhatHangHoaActivity.this, "Cập nhật không thành công");
            }
        }
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
                ivHinhAnh.setImageBitmap(bitmap);
            } catch (Exception e) {
                e.getMessage();
            }
        }
    }

}