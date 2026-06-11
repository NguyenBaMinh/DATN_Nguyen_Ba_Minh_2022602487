package app.edu.app.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.util.Map;

import app.edu.app.R;
import app.edu.app.utils.MyToast;
import app.edu.app.utils.VNPayHelper;

/**
 * Activity để hiển thị VNPay payment trong WebView
 */
public class VNPayPaymentActivity extends AppCompatActivity {
    private static final String TAG = "VNPayPaymentActivity";
    
    private WebView webView;
    private ProgressBar progressBar;
    private Toolbar toolbar;
    
    private String paymentUrl;
    private String orderId;
    private long amount;
    private String hoaDonId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vnpay_payment);
        
        // Lấy data từ intent
        paymentUrl = getIntent().getStringExtra("payment_url");
        orderId = getIntent().getStringExtra("order_id");
        amount = getIntent().getLongExtra("amount", 0);
        hoaDonId = getIntent().getStringExtra("hoa_don_id");
        
        if (paymentUrl == null || paymentUrl.isEmpty()) {
            MyToast.error(this, "Không thể tải trang thanh toán");
            finish();
            return;
        }
        
        initToolbar();
        initViews();
        setupWebView();
        loadPaymentUrl();
    }

    private void initToolbar() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Thanh toán VNPay");
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void initViews() {
        webView = findViewById(R.id.webView);
        progressBar = findViewById(R.id.progressBar);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setSupportZoom(true);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setDisplayZoomControls(false);
        
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                Log.d(TAG, "Loading URL: " + url);
                
                // Kiểm tra nếu là return URL từ VNPay
                if (url.contains("vnpay_return") || url.contains("vnp_ResponseCode")) {
                    handleVNPayReturn(url);
                    return true;
                }
                
                return false;
            }

            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                progressBar.setVisibility(View.VISIBLE);
                
                // Kiểm tra return URL
                if (url.contains("vnpay_return") || url.contains("vnp_ResponseCode")) {
                    handleVNPayReturn(url);
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void loadPaymentUrl() {
        Log.d(TAG, "Loading payment URL: " + paymentUrl);
        webView.loadUrl(paymentUrl);
    }

    /**
     * Xử lý return URL từ VNPay
     */
    private void handleVNPayReturn(String returnUrl) {
        Log.d(TAG, "Handling VNPay return URL: " + returnUrl);
        
        // Parse parameters từ URL
        Map<String, String> vnpParams = VNPayHelper.parseReturnUrl(returnUrl);
        
        // Verify signature
        boolean isValid = VNPayHelper.verifyReturnUrl(vnpParams);
        
        if (!isValid) {
            MyToast.error(this, "Xác thực thanh toán thất bại");
            setResult(RESULT_CANCELED);
            finish();
            return;
        }
        
        // Kiểm tra trạng thái giao dịch
        VNPayHelper.PaymentResult result = VNPayHelper.checkTransactionStatus(vnpParams);
        
        // Tạo intent result
        Intent resultIntent = new Intent();
        resultIntent.putExtra("success", result.success);
        resultIntent.putExtra("message", result.message);
        resultIntent.putExtra("transaction_id", result.transactionId);
        resultIntent.putExtra("order_id", result.orderId);
        resultIntent.putExtra("amount", result.amount);
        resultIntent.putExtra("hoa_don_id", hoaDonId);
        
        if (result.success) {
            MyToast.successful(this, result.message);
            setResult(RESULT_OK, resultIntent);
        } else {
            MyToast.error(this, result.message);
            setResult(RESULT_CANCELED, resultIntent);
        }
        
        finish();
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            // Hủy thanh toán
            setResult(RESULT_CANCELED);
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
        }
        super.onDestroy();
    }
}

