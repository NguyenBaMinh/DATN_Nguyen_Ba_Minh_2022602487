package app.edu.app.ui;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.TextView;

import app.edu.app.R;
import app.edu.app.utils.MyToast;

public class LienHeActivity extends AppCompatActivity {
    Toolbar toolbar;
    TextView tvLienHePhu, tvLienHeAn, tvLienHeTin;
    public static  final int MY_PERMISSIONS_REQUEST_CALL_PHONE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lien_he);
        initView();
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
        callAndSendEmail(tvLienHePhu, "0999999999", "Levovuong@gmail.com");

    }

    private void initView() {
        toolbar = findViewById(R.id.toolbar);
        tvLienHePhu = findViewById(R.id.tvLienHePhu);

    }

    private void callAndSendEmail(TextView tvLienHe, String tell, String email) {
        tvLienHe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Creating the instance of PopupMenu
                PopupMenu popup = new PopupMenu(LienHeActivity.this, tvLienHe);
                //Inflating the Popup using xml file
                popup.getMenuInflater()
                        .inflate(R.menu.menu_setting_lienhe, popup.getMenu());
                //registering popup with OnMenuItemClickListener
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    public boolean onMenuItemClick(MenuItem item) {

                        if (item.getItemId() == R.id.itSoDienThoai) {
                            call(tell);
                        }
                        if (item.getItemId() == R.id.itEmail) {
                            sendEmail(email);
                        }
                        return true;
                    }
                });
                popup.show(); //showing popup menu
            }
        });
    }

    public void sendEmail(String email) {
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("message/rfc822");
        i.putExtra(Intent.EXTRA_EMAIL, new String[]{email});
        i.putExtra(Intent.EXTRA_SUBJECT, "subject of email");
        i.putExtra(Intent.EXTRA_TEXT, "body of email");
        try {
            startActivity(Intent.createChooser(i, "Send mail..."));
        } catch (android.content.ActivityNotFoundException ex) {
            MyToast.successful(LienHeActivity.this, "There are no email clients installed.");
        }
    }

    public void call(String tell) {
        try {
            Intent callIntent = new Intent(Intent.ACTION_CALL);
            callIntent.setData(Uri.parse("tel:" + tell));

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) !=
                    PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CALL_PHONE},
                        MY_PERMISSIONS_REQUEST_CALL_PHONE);

                return;
            }

            startActivity(callIntent);
        } catch (ActivityNotFoundException e) {
            Log.e("helloAndroid", "Call failed", e);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.anim_in_left, R.anim.anim_out_right);
    }
}