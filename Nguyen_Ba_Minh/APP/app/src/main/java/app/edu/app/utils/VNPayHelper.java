package app.edu.app.utils;

import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import app.edu.app.config.VNPayConfig;

/**
 * VNPay Helper Class
 * Xử lý tạo payment URL và verify return URL từ VNPay
 */
public class VNPayHelper {
    private static final String TAG = "VNPayHelper";

    /**
     * Tạo payment URL cho VNPay
     * @param orderId Mã đơn hàng
     * @param amount Số tiền (VND)
     * @param orderInfo Thông tin đơn hàng
     * @param ipAddr Địa chỉ IP
     * @param bankCode Mã ngân hàng (optional)
     * @return Payment URL
     */
    public static String createPaymentUrl(String orderId, long amount, String orderInfo, 
                                         String ipAddr, String bankCode) {
        try {
            Map<String, String> vnp_Params = new HashMap<>();
            
            // Tạo createDate
            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
            formatter.setTimeZone(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
            String createDate = formatter.format(new Date());
            
            vnp_Params.put("vnp_Version", VNPayConfig.VNPAY_VERSION);
            vnp_Params.put("vnp_Command", VNPayConfig.VNPAY_COMMAND);
            vnp_Params.put("vnp_TmnCode", VNPayConfig.VNPAY_TMN_CODE);
            vnp_Params.put("vnp_Locale", VNPayConfig.VNPAY_LOCALE);
            vnp_Params.put("vnp_CurrCode", VNPayConfig.VNPAY_CURR_CODE);
            vnp_Params.put("vnp_TxnRef", orderId);
            vnp_Params.put("vnp_OrderInfo", orderInfo);
            vnp_Params.put("vnp_OrderType", VNPayConfig.VNPAY_ORDER_TYPE);
            vnp_Params.put("vnp_Amount", String.valueOf(amount * 100)); // VNPay yêu cầu số tiền nhỏ nhất
            vnp_Params.put("vnp_ReturnUrl", VNPayConfig.VNPAY_RETURN_URL);
            vnp_Params.put("vnp_IpAddr", ipAddr != null ? ipAddr : "127.0.0.1");
            vnp_Params.put("vnp_CreateDate", createDate);
            
            // Thêm bankCode nếu có
            if (bankCode != null && !bankCode.trim().isEmpty()) {
                vnp_Params.put("vnp_BankCode", bankCode);
            }
            
            Log.d(TAG, "Original params: " + vnp_Params.toString());
            
            // Sắp xếp và encode params (không có vnp_SecureHash)
            // Match với Node.js: sortObject(vnp_Params)
            Map<String, String> sortedParams = sortParams(vnp_Params);
            
            Log.d(TAG, "Sorted params (keys only): " + sortedParams.keySet().toString());
            
            // Tạo query string để hash
            // Match với Node.js: querystring.stringify(vnp_Params, { encode: false })
            String signData = createQueryString(sortedParams);
            
            Log.d(TAG, "Sign data for hash: " + signData);
            
            // Tạo secure hash
            // Match với Node.js: hmac.update(Buffer.from(signData, 'utf-8')).digest('hex')
            String vnp_SecureHash = hmacSHA512(VNPayConfig.VNPAY_HASH_SECRET, signData);
            
            Log.d(TAG, "Generated hash: " + vnp_SecureHash);
            
            // Thêm vnp_SecureHash vào original params (không phải sorted params)
            // Match với Node.js: vnp_Params['vnp_SecureHash'] = signed
            vnp_Params.put("vnp_SecureHash", vnp_SecureHash);
            
            Log.d(TAG, "Params with hash: " + vnp_Params.toString());
            
            // Sort lại tất cả params (bao gồm vnp_SecureHash)
            // Match với Node.js: querystring.stringify(vnp_Params, { encode: false })
            sortedParams = sortParams(vnp_Params);
            
            // Tạo final query string
            String finalQuery = createQueryString(sortedParams);
            String paymentUrl = VNPayConfig.VNPAY_URL + "?" + finalQuery;
            
            Log.d(TAG, "Final query string: " + finalQuery);
            Log.d(TAG, "Payment URL created: " + paymentUrl);
            return paymentUrl;
            
        } catch (Exception e) {
            Log.e(TAG, "Error creating payment URL", e);
            return null;
        }
    }

    /**
     * Verify return URL từ VNPay
     * @param vnpParams Map chứa các params từ VNPay return URL
     * @return true nếu verify thành công
     */
    public static boolean verifyReturnUrl(Map<String, String> vnpParams) {
        try {
            String vnp_SecureHash = vnpParams.get("vnp_SecureHash");
            
            if (vnp_SecureHash == null || vnp_SecureHash.isEmpty()) {
                return false;
            }
            
            // Xóa secureHash khỏi params để verify
            Map<String, String> paramsToVerify = new HashMap<>(vnpParams);
            paramsToVerify.remove("vnp_SecureHash");
            paramsToVerify.remove("vnp_SecureHashType");
            
            // Sắp xếp và encode params
            Map<String, String> sortedParams = sortParams(paramsToVerify);
            String queryUrl = createQueryString(sortedParams);
            
            Log.d(TAG, "Query string for verify: " + queryUrl);
            
            // Tạo hash và so sánh
            String signed = hmacSHA512(VNPayConfig.VNPAY_HASH_SECRET, queryUrl);
            
            Log.d(TAG, "Expected hash: " + vnp_SecureHash);
            Log.d(TAG, "Calculated hash: " + signed);
            
            return vnp_SecureHash.equals(signed);
            
        } catch (Exception e) {
            Log.e(TAG, "Error verifying return URL", e);
            return false;
        }
    }

    /**
     * Kiểm tra trạng thái giao dịch
     * @param vnpParams Map chứa params từ VNPay
     * @return PaymentResult
     */
    public static PaymentResult checkTransactionStatus(Map<String, String> vnpParams) {
        String responseCode = vnpParams.get("vnp_ResponseCode");
        
        if (VNPayConfig.RESPONSE_CODE_SUCCESS.equals(responseCode)) {
            return new PaymentResult(true, "Giao dịch thành công", vnpParams);
        } else {
            String message = getResponseMessage(responseCode);
            return new PaymentResult(false, message, vnpParams);
        }
    }

    /**
     * Lấy thông báo lỗi dựa trên response code
     */
    private static String getResponseMessage(String code) {
        if (code == null) {
            return "Lỗi không xác định";
        }
        
        switch (code) {
            case "00":
                return "Giao dịch thành công";
            case "07":
                return "Trừ tiền thành công. Giao dịch bị nghi ngờ (liên quan tới lừa đảo, giao dịch bất thường).";
            case "09":
                return "Giao dịch không thành công do: Thẻ/Tài khoản của khách hàng chưa đăng ký dịch vụ InternetBanking tại ngân hàng.";
            case "10":
                return "Giao dịch không thành công do: Khách hàng xác thực thông tin thẻ/tài khoản không đúng quá 3 lần";
            case "11":
                return "Giao dịch không thành công do: Đã hết hạn chờ thanh toán. Xin quý khách vui lòng thực hiện lại giao dịch.";
            case "12":
                return "Giao dịch không thành công do: Thẻ/Tài khoản của khách hàng bị khóa.";
            case "13":
                return "Giao dịch không thành công do Quý khách nhập sai mật khẩu xác thực giao dịch (OTP).";
            case "24":
                return "Giao dịch không thành công do: Khách hàng hủy giao dịch";
            case "51":
                return "Giao dịch không thành công do: Tài khoản của quý khách không đủ số dư để thực hiện giao dịch.";
            case "65":
                return "Giao dịch không thành công do: Tài khoản của Quý khách đã vượt quá hạn mức giao dịch trong ngày.";
            case "75":
                return "Ngân hàng thanh toán đang bảo trì.";
            case "79":
                return "Giao dịch không thành công do: KH nhập sai mật khẩu thanh toán quá số lần quy định.";
            case "99":
                return "Các lỗi khác";
            default:
                return "Lỗi không xác định (Code: " + code + ")";
        }
    }

    /**
     * Sắp xếp params theo thứ tự alphabet và encode
     * Match chính xác với logic Node.js sortObject:
     * 1. Encode tất cả keys, push vào array
     * 2. Sort array encoded keys
     * 3. Với mỗi encoded key, tìm original key và encode value, replace %20 thành +
     */
    private static Map<String, String> sortParams(Map<String, String> params) {
        try {
            // Bước 1: Encode tất cả keys và push vào array (giống Node.js)
            List<String> encodedKeys = new ArrayList<>();
            Map<String, String> encodedToOriginal = new HashMap<>(); // Map encoded key -> original key
            
            for (String originalKey : params.keySet()) {
                String encodedKey = URLEncoder.encode(originalKey, StandardCharsets.UTF_8.toString());
                if (!encodedToOriginal.containsKey(encodedKey)) { // Tránh duplicate nếu có collision
                    encodedKeys.add(encodedKey);
                    encodedToOriginal.put(encodedKey, originalKey);
                }
            }
            
            // Bước 2: Sort encoded keys (giống Node.js: str.sort())
            Collections.sort(encodedKeys);
            
            // Bước 3: Tạo sorted map
            // Giống Node.js: sorted[str[key]] = encodeURIComponent(obj[str[key]]).replace(/%20/g, '+')
            Map<String, String> sortedParams = new LinkedHashMap<>();
            for (String encodedKey : encodedKeys) {
                String originalKey = encodedToOriginal.get(encodedKey);
                String originalValue = params.get(originalKey);
                // Encode value và replace %20 thành +
                String encodedValue = URLEncoder.encode(
                        originalValue != null ? originalValue : "", 
                        StandardCharsets.UTF_8.toString())
                        .replace("%20", "+");
                sortedParams.put(encodedKey, encodedValue);
            }
            
            return sortedParams;
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Error sorting params", e);
            return params;
        }
    }

    /**
     * Tạo query string từ params (không encode lại vì đã encode trong sortParams)
     */
    private static String createQueryString(Map<String, String> params) {
        List<String> queryParts = new ArrayList<>();
        
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String value = entry.getValue() != null ? entry.getValue() : "";
            queryParts.add(entry.getKey() + "=" + value);
        }
        
        return String.join("&", queryParts);
    }

    /**
     * Tạo HMAC SHA512 hash
     */
    private static String hmacSHA512(String key, String data) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA512");
            javax.crypto.spec.SecretKeySpec secretKeySpec = new javax.crypto.spec.SecretKeySpec(
                    key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            Log.e(TAG, "Error creating HMAC SHA512", e);
            return "";
        }
    }

    /**
     * Parse URL parameters từ return URL
     */
    public static Map<String, String> parseReturnUrl(String returnUrl) {
        Map<String, String> params = new HashMap<>();
        
        try {
            if (returnUrl == null || returnUrl.isEmpty()) {
                return params;
            }
            
            String query = returnUrl;
            int questionMarkIndex = returnUrl.indexOf('?');
            if (questionMarkIndex >= 0) {
                query = returnUrl.substring(questionMarkIndex + 1);
            }
            
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                int equalIndex = pair.indexOf('=');
                if (equalIndex > 0) {
                    String key = java.net.URLDecoder.decode(pair.substring(0, equalIndex), 
                            StandardCharsets.UTF_8.toString());
                    String value = java.net.URLDecoder.decode(pair.substring(equalIndex + 1), 
                            StandardCharsets.UTF_8.toString());
                    params.put(key, value);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing return URL", e);
        }
        
        return params;
    }

    /**
     * Class để lưu kết quả payment
     */
    public static class PaymentResult {
        public boolean success;
        public String message;
        public Map<String, String> params;
        public String transactionId;
        public String amount;
        public String orderId;

        public PaymentResult(boolean success, String message, Map<String, String> params) {
            this.success = success;
            this.message = message;
            this.params = params;
            
            if (params != null) {
                this.transactionId = params.get("vnp_TransactionNo");
                this.amount = params.get("vnp_Amount");
                this.orderId = params.get("vnp_TxnRef");
            }
        }
    }
}

