package app.edu.app;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import com.google.android.material.bottomnavigation.BottomNavigationItemView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

import java.util.ArrayList;

import app.edu.app.adapter.ViewPagerMainAdapter;
import app.edu.app.dao.ThongBaoDAO;
import app.edu.app.model.ThongBao;
import app.edu.app.ui.SignInActivity;


public class MainActivity extends AppCompatActivity {
    private String keyUser = ""; // Mã người dùng
    ViewPager2 vp2Main;
    BottomNavigationView bnvMain;
    View iconNotification;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initViewPager2Main();
        setKeyUser();
        showIconNotification();

        bnvMain.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @SuppressLint("NonConstantResourceId")
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
//                switch (item.getItemId()) {
//                    case R.id.menu_home:
//                        // Open fragment Home
//                        vp2Main.setCurrentItem(0, false);
//                        break;
//                    case R.id.menu_search:
//                        // Open fragment Search
//                        vp2Main.setCurrentItem(1, false);
//                        break;
//                    case R.id.menu_notification:
//                        // Open fragment Messenger
//                        vp2Main.setCurrentItem(2, false);
//                        break;
//                    case R.id.menu_setting:
//                        // Open fragment Setting
//                        vp2Main.setCurrentItem(3, false);
//                        break;
//                }
                if (item.getItemId() == R.id.menu_home) {
                    // Open fragment Home
                    vp2Main.setCurrentItem(0, false);
                } else if (item.getItemId() == R.id.menu_search) {
                    // Open fragment Search
                    vp2Main.setCurrentItem(1, false);
                } else if (item.getItemId() == R.id.menu_notification) {
                    // Open fragment Messenger
                    vp2Main.setCurrentItem(2, false);
                } else if (item.getItemId() == R.id.menu_setting) {
                    // Open fragment Setting
                    vp2Main.setCurrentItem(3, false);
                }
                checkStatusNotification();
                return true;
            }
        });

    }

    private void initView() {
        bnvMain = findViewById(R.id.bnvMain);
        vp2Main = findViewById(R.id.viewPager2Main);
    }

    private void initViewPager2Main() {
        ViewPagerMainAdapter adapter = new ViewPagerMainAdapter(this);

        vp2Main.setUserInputEnabled(false);
        vp2Main.setOffscreenPageLimit(3);
        vp2Main.setAdapter(adapter);
    }

    @SuppressLint("InflateParams")
    private void showIconNotification() {
        // Show icon khi có thông báo có trạng thái chưa xem
        BottomNavigationItemView itemView = bnvMain.findViewById(R.id.menu_notification);
        iconNotification = getLayoutInflater().inflate(R.layout.layout_ic_thongbao, null);
        checkStatusNotification();

        // Thêm icon vào item Thông báo
        itemView.addView(iconNotification);
    }

    private void checkStatusNotification() {
        ThongBaoDAO thongBaoDAO = new ThongBaoDAO(this);
        
        // Lấy thông báo chưa xem của user hiện tại (bao gồm thông báo chung)
        ArrayList<ThongBao> allUnreadNotifications = thongBaoDAO.getByTrangThaiChuaXem();
        
        // Lọc theo user hiện tại
        ArrayList<ThongBao> userUnreadNotifications = new ArrayList<>();
        for (ThongBao tb : allUnreadNotifications) {
            // Hiển thị nếu:
            // 1. Thông báo chung (maNguoiDung == null)
            // 2. Thông báo riêng cho user này (maNguoiDung == keyUser)
            if (tb.getMaNguoiDung() == null || 
                (keyUser != null && keyUser.equals(tb.getMaNguoiDung()))) {
                userUnreadNotifications.add(tb);
            }
        }

        if (userUnreadNotifications.size() == 0) {
            // Ẩn thông báo
            iconNotification.setVisibility(View.GONE);
        } else {
            // Hiện thông báo
            iconNotification.setVisibility(View.VISIBLE);
        }
    }


    private void setKeyUser() {
        Intent intent = this.getIntent();
        keyUser = intent.getStringExtra(SignInActivity.KEY_USER);
        
        // Nếu không có trong Intent, lấy từ SharedPreferences
        if (keyUser == null || keyUser.isEmpty()) {
            sharedPreferences = getSharedPreferences("USER_FILE", MODE_PRIVATE);
            keyUser = sharedPreferences.getString("maNguoiDung", "");
        }
    }

    public String getKeyUser() {
        return keyUser;
    }
    
    /**
     * Kiểm tra xem user đã đăng nhập chưa
     */
    public boolean isUserLoggedIn() {
        return keyUser != null && !keyUser.isEmpty();
    }
    
    /**
     * Yêu cầu đăng nhập - mở SignInActivity
     */
    public void requireLogin() {
        Intent intent = new Intent(this, SignInActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.anim_in_right, R.anim.anim_out_left);
    }

    @Override
    public void onBackPressed() {
        // Open fragment Home
        vp2Main.setCurrentItem(0, false);
        bnvMain.setSelectedItemId(R.id.menu_home);
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkStatusNotification();
    }
}