package app.edu.app.ui;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.material.textfield.TextInputLayout;

import java.util.Objects;

import app.edu.app.MainActivity;

import app.edu.app.R;
import app.edu.app.dao.NguoiDungDAO;
import app.edu.app.notification.MyNotification;
import app.edu.app.utils.Loading;
import app.edu.app.utils.MyToast;

public class SignInActivity extends AppCompatActivity implements View.OnClickListener {
    public static final String KEY_USER = "maNguoiDung";

    Loading loading;
    TextInputLayout tilUserName, tilPassword;
    Button btnSignIn;
    TextView tvSignUp, tvForgotPassword;
    NguoiDungDAO nguoiDungDAO;
    public static String _maNguoiDung = "";
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);
        initView();

        nguoiDungDAO = new NguoiDungDAO(SignInActivity.this);
        sharedPreferences = getSharedPreferences("USER_FILE", MODE_PRIVATE);
        btnSignIn.setOnClickListener(this);
        tvSignUp.setOnClickListener(this);
        tvForgotPassword.setOnClickListener(this);
        loading = new Loading(SignInActivity.this);
    }

    private void initView() {
        tilUserName = findViewById(R.id.tilUserName);
        tilPassword = findViewById(R.id.tilPassword);
        btnSignIn = findViewById(R.id.btnSignIn);
        tvSignUp = findViewById(R.id.tvSignUp);
        tvForgotPassword = findViewById(R.id.tvQuenMatKhau);
    }

    @NonNull
    private String getText(TextInputLayout textInputLayout) {
        return Objects.requireNonNull(textInputLayout.getEditText()).getText().toString().trim();
    }

    private void loginSystem() {
        loading.startLoading();
        String username = getText(tilUserName);
        String password = getText(tilPassword);

        // Xử lý đăng nhập
        if (!username.isEmpty() && !password.isEmpty()) {
            if (nguoiDungDAO.checkLogin(username, password)) {
                // Đăng nhập thành công
                MyToast.successful(SignInActivity.this, "Đăng nhập thành công");
                MyNotification.getNotification(SignInActivity.this, "Đăng nhập hệ thống thành công");
                openMainActivity(username);
            } else {
                // Đăng nhập thất bại
                MyToast.error(SignInActivity.this, "Thông tin đăng nhập không đúng-vui lòng nhập lại");
                loading.stopLoading();
            }
        } else {
            // Thông tin không hợp lệ
            MyToast.error(SignInActivity.this, "Không để trống mật khẩu hoặc tên đăng nhập");
            loading.stopLoading();
        }
    }

    private void openMainActivity(String username) {
        Intent intent = new Intent(SignInActivity.this, MainActivity.class);
        intent.putExtra(KEY_USER, username);
        _maNguoiDung = username;
        sharedPreferences.edit().putString("maNguoiDung", username).apply();
        startActivity(intent);
        overridePendingTransition(R.anim.anim_in_right, R.anim.anim_out_left);
    }

    private void openSignUpActivity() {
        startActivity(new Intent(SignInActivity.this, SignUpActivity.class));
        overridePendingTransition(R.anim.anim_in_right, R.anim.anim_out_left);
    }

    private void openQuenMatKhauActivity() {
        startActivity(new Intent(SignInActivity.this, QuenMatKhauActivity.class));
        overridePendingTransition(R.anim.anim_in_right, R.anim.anim_out_left);
    }

    @Override
    public void onBackPressed() {
        // Giữ trống để ngăn việc quay lại màn hình trước đó
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.btnSignIn) {
            loginSystem();
        } else if (view.getId() == R.id.tvSignUp) {
            openSignUpActivity();
        } else if (view.getId() == R.id.tvQuenMatKhau) {
            openQuenMatKhauActivity();
        }
    }
}