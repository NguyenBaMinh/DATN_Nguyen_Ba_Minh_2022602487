package app.edu.app.ui;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import app.edu.app.R;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    public static final int TIME_OUT = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // Mở MainActivity trực tiếp để hiển thị trang chủ
                startActivity(new Intent(SplashActivity.this, app.edu.app.MainActivity.class));
                overridePendingTransition(R.anim.anim_in_right, R.anim.anim_out_left);
                finish();
            }
        }, TIME_OUT);
    }
}