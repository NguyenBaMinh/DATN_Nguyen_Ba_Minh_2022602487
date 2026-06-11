package app.edu.app.config;

/**
 * VNPay Configuration
 * Lưu các thông tin cấu hình VNPay
 */
public class VNPayConfig {
    // Sandbox credentials
    public static final String VNPAY_TMN_CODE = "6CO9MA53";
    public static final String VNPAY_HASH_SECRET = "XUEB3HJFMD1OQEDT1GEJ0IC72X92N0X4";
    public static final String VNPAY_URL = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
    // Return URL cho Android app - VNPay sẽ redirect về URL này sau khi thanh toán
    // Trong Android, chúng ta sẽ bắt URL này trong WebView
    public static final String VNPAY_RETURN_URL = "https://sandbox.vnpayment.vn/return";
    public static final String VNPAY_API_URL = "https://sandbox.vnpayment.vn/merchant_webapi/api/transaction";
    
    // Version
    public static final String VNPAY_VERSION = "2.1.0";
    public static final String VNPAY_COMMAND = "pay";
    public static final String VNPAY_CURR_CODE = "VND";
    public static final String VNPAY_LOCALE = "vn";
    public static final String VNPAY_ORDER_TYPE = "billpayment";
    
    // Response codes
    public static final String RESPONSE_CODE_SUCCESS = "00";
    
    // Note: Trong production, nên lưu các giá trị này trong file config hoặc server
    // và lấy từ API để bảo mật hơn
}

