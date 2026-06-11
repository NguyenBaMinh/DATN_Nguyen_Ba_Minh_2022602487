package app.edu.app.fragment;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.io.InputStream;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;
import app.edu.app.MainActivity;
import app.edu.app.R;
import app.edu.app.dao.NguoiDungDAO;
import app.edu.app.interfaces.OnUrlFetchedListener;
import app.edu.app.model.NguoiDung;
import app.edu.app.ui.DoiMatKhauActivity;
import app.edu.app.ui.LienHeActivity;
import app.edu.app.ui.SignInActivity;
import app.edu.app.ui.ThietLapTaiKhoanActivity;
import app.edu.app.utils.ImageCache;
import app.edu.app.utils.ImageToByte;
import app.edu.app.utils.MyToast;

public class SettingFragment extends Fragment implements View.OnClickListener {

    public static final int PICK_IMAGE = 1;
    public static final String MA_NGUOIDUNG = "MA_NGUOIDUNG";

    TextView tvLienHe, tvThietLapTaiKhoan, tvDoiMatKhuau, tvDangXuat, tvTenNguoiDung, tvChucVu, tvEmail;
    MainActivity mainActivity;
    NguoiDungDAO nguoiDungDAO;
    CircleImageView civHinhAnh;
    ImageView ivDoiHinhAnh;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_setting, container, false);
        initView(view);

        mainActivity = ((MainActivity) getActivity());
        nguoiDungDAO = new NguoiDungDAO(getContext());

        fillActivity();

        tvLienHe.setOnClickListener(this);
        tvThietLapTaiKhoan.setOnClickListener(this);
        tvDoiMatKhuau.setOnClickListener(this);
        tvDangXuat.setOnClickListener(this);
        ivDoiHinhAnh.setOnClickListener(this);

        return view;
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View view) {
        // Kiểm tra login cho các action yêu cầu đăng nhập
        if (view.getId() == R.id.ivDoiHinhAnh) {
            if (!checkLoginRequired()) return;
            requestPermissionPickImage();
        } else if (view.getId() == R.id.tvLienHe) {
            openLienHeActivity();
        } else if (view.getId() == R.id.tvThietLapTaiKhoan) {
            if (!checkLoginRequired()) return;
            openTLTKActivity();
        } else if (view.getId() == R.id.tvDoiMatKhau) {
            if (!checkLoginRequired()) return;
            openDoiMatKhauAcitvity();
        } else if (view.getId() == R.id.tvDangXuat) {
            logout();
        }
    }
    
    /**
     * Kiểm tra xem user đã đăng nhập chưa
     * @return true nếu đã đăng nhập, false nếu chưa
     */
    private boolean checkLoginRequired() {
        if (mainActivity != null && !mainActivity.isUserLoggedIn()) {
            MyToast.error(getContext(), "Vui lòng đăng nhập để sử dụng chức năng này");
            openSignInActivity();
            return false;
        }
        return true;
    }

    private void initView(View view) {
        tvLienHe = view.findViewById(R.id.tvLienHe);
        tvThietLapTaiKhoan = view.findViewById(R.id.tvThietLapTaiKhoan);
        tvDoiMatKhuau = view.findViewById(R.id.tvDoiMatKhau);
        tvDangXuat = view.findViewById(R.id.tvDangXuat);
        civHinhAnh = view.findViewById(R.id.civHinhAnh);
        tvTenNguoiDung = view.findViewById(R.id.tvTenNguoiDung);
        tvChucVu = view.findViewById(R.id.tvChucVu);
        tvEmail = view.findViewById(R.id.tvEmail);
        ivDoiHinhAnh = view.findViewById(R.id.ivDoiHinhAnh);
    }

    private void fillActivity() {
        // Kiểm tra xem user đã đăng nhập chưa
        if (mainActivity != null && !mainActivity.isUserLoggedIn()) {
            // Chưa đăng nhập - hiển thị thông tin mặc định
            civHinhAnh.setImageResource(R.drawable.avatar_user);
            tvTenNguoiDung.setText("Khách");
            tvChucVu.setText("Vui lòng đăng nhập");
            tvEmail.setText("");
            
            // Ẩn các chức năng yêu cầu đăng nhập
            tvThietLapTaiKhoan.setVisibility(View.GONE);
            tvDoiMatKhuau.setVisibility(View.GONE);
            ivDoiHinhAnh.setVisibility(View.GONE);
            
            // Đổi text nút đăng xuất thành đăng nhập
            tvDangXuat.setText("Đăng nhập");
            return;
        }
        
        NguoiDung nguoiDung = getNguoiDung();
        if (nguoiDung == null) {
            // Không lấy được thông tin user
            civHinhAnh.setImageResource(R.drawable.avatar_user);
            tvTenNguoiDung.setText("Khách");
            tvChucVu.setText("Vui lòng đăng nhập");
            tvEmail.setText("");
            return;
        }
        
        // Đã đăng nhập - hiển thị đầy đủ thông tin
        ImageCache.getUrlFromCache("nguoidung_" + nguoiDung.getMaNguoiDung(), new OnUrlFetchedListener() {
            @Override
            public void onUrlFetched(String url) {
                if (url != null) {
                    Picasso.get().load(url).into(civHinhAnh);
                }
            }
        });

        tvTenNguoiDung.setText(nguoiDung.getHoVaTen());
        tvChucVu.setText(nguoiDung.getChucVu());
        tvEmail.setText(nguoiDung.getEmail());
        
        // Hiện các chức năng
        tvThietLapTaiKhoan.setVisibility(View.VISIBLE);
        tvDoiMatKhuau.setVisibility(View.VISIBLE);
        ivDoiHinhAnh.setVisibility(View.VISIBLE);
        tvDangXuat.setText("Đăng xuất");
    }

    private NguoiDung getNguoiDung() {
        // Lấy mã người dùng từ MainActivity theo fun(getKeyUser)
        String maNguoiDung = Objects.requireNonNull(mainActivity).getKeyUser();
        return nguoiDungDAO.getByMaNguoiDung(maNguoiDung);
    }

    private void requestPermissionPickImage() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED) {
            // cấp quyền cho ứng dụng nếu chưa được cấp quyền
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    1);
        } else {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE);
        }
    }

    private void openLienHeActivity() {
        startActivity(new Intent(getContext(), LienHeActivity.class));
        ((Activity) requireContext()).overridePendingTransition(R.anim.anim_in_right, R.anim.anim_out_left);
    }

    private void openTLTKActivity() {
        NguoiDung nguoiDung = getNguoiDung();
        if (nguoiDung == null) return;
        
        Intent intent = new Intent(getContext(), ThietLapTaiKhoanActivity.class);
        intent.putExtra(MA_NGUOIDUNG, nguoiDung.getMaNguoiDung());
        startActivity(intent);
        ((Activity) requireContext()).overridePendingTransition(R.anim.anim_in_right, R.anim.anim_out_left);
    }

    private void openDoiMatKhauAcitvity() {
        NguoiDung nguoiDung = getNguoiDung();
        if (nguoiDung == null) return;
        
        Intent intent = new Intent(getContext(), DoiMatKhauActivity.class);
        intent.putExtra(MA_NGUOIDUNG, nguoiDung.getMaNguoiDung());
        startActivity(intent);
        ((Activity) requireContext()).overridePendingTransition(R.anim.anim_in_right, R.anim.anim_out_left);
    }

    private void openSignInActivity() {
        startActivity(new Intent(getContext(), SignInActivity.class));
        ((Activity) requireContext()).overridePendingTransition(R.anim.anim_in_left, R.anim.anim_out_right);
    }

    private void updateAvatarUser(){
        NguoiDung nguoiDung = getNguoiDung();
        if (nguoiDung == null) {
            MyToast.error(getContext(), "Vui lòng đăng nhập để cập nhật ảnh đại diện");
            return;
        }
        
        nguoiDung.setHinhAnh(ImageToByte.circleImageViewToByte(getContext(), civHinhAnh));

        if (nguoiDungDAO.updateNguoiDung(nguoiDung)) {
            MyToast.successful(getContext(), "Cập nhật ảnh đại diện thành công");
            fillActivity();
        } else {
            MyToast.error(getContext(), "Lỗi");
        }
    }

    private void logout() {
        // Nếu chưa đăng nhập, mở màn hình đăng nhập
        if (mainActivity != null && !mainActivity.isUserLoggedIn()) {
            openSignInActivity();
            return;
        }
        
        // Đã đăng nhập - hiển thị dialog xác nhận đăng xuất
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.AlertDialogTheme)
                .setMessage("Bạn có muốn đăng xuất?")
                .setPositiveButton("Đăng xuất", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // Xóa thông tin đăng nhập
                        if (getContext() != null) {
                            getContext().getSharedPreferences("USER_FILE", getContext().MODE_PRIVATE)
                                    .edit().remove("maNguoiDung").apply();
                        }
                        // Mở màn hình đăng nhập
                        openSignInActivity();
                    }
                })
                .setNegativeButton("Huỷ", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            try {
                InputStream stream = requireContext().getContentResolver().openInputStream(uri);
                Bitmap bitmap = BitmapFactory.decodeStream(stream);
                civHinhAnh.setImageBitmap(bitmap);
                updateAvatarUser();
            } catch (Exception e) {
                e.getMessage();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        fillActivity();
    }
}