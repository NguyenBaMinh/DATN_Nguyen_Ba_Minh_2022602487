package app.edu.app.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.cardview.widget.CardView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import app.edu.app.R;
import app.edu.app.dao.HangHoaDAO;
import app.edu.app.dao.HoaDonChiTietDAO;
import app.edu.app.model.HangHoa;
import app.edu.app.model.HoaDonChiTiet;
import app.edu.app.utils.AISuggestionHelper;
import app.edu.app.utils.MyToast;

/**
 * Dialog để hiển thị gợi ý đồ uống từ AI
 */
public class AISuggestionDialog {
    private static final String TAG = "AISuggestionDialog";
    
    private Dialog dialog;
    private Context context;
    private AISuggestionHelper suggestionHelper;
    private String maKhachHang;
    private String maHoaDon;
    
    // DAOs
    private HangHoaDAO hangHoaDAO;
    private HoaDonChiTietDAO hoaDonChiTietDAO;
    
    // Views
    private LinearLayout llLoading;
    private LinearLayout llSuggestionsList;
    private CardView cardContent;
    private CardView cardError;
    private TextView tvSuggestion;
    private TextView tvError;
    private Button btnRetry;
    private ImageView ivClose;
    private TextView tvClose;
    
    public AISuggestionDialog(Context context) {
        this(context, null);
    }
    
    public AISuggestionDialog(Context context, String maHoaDon) {
        this.context = context;
        this.maHoaDon = maHoaDon;
        this.suggestionHelper = new AISuggestionHelper(context);
        this.hangHoaDAO = new HangHoaDAO(context);
        this.hoaDonChiTietDAO = new HoaDonChiTietDAO(context);
        
        // Lấy mã khách hàng từ SharedPreferences
        SharedPreferences sharedPreferences = context.getSharedPreferences("USER_FILE", Context.MODE_PRIVATE);
        this.maKhachHang = sharedPreferences.getString("maNguoiDung", "");
        
        initDialog();
    }
    
    private void initDialog() {
        dialog = new Dialog(context);
        dialog.setContentView(R.layout.dialog_ai_suggestion);
        
        // Setup views
        llLoading = dialog.findViewById(R.id.llLoading);
        cardContent = dialog.findViewById(R.id.cardContent);
        cardError = dialog.findViewById(R.id.cardError);
        tvSuggestion = dialog.findViewById(R.id.tvSuggestion);
        tvError = dialog.findViewById(R.id.tvError);
        btnRetry = dialog.findViewById(R.id.btnRetry);
        ivClose = dialog.findViewById(R.id.ivClose);
        tvClose = dialog.findViewById(R.id.tvClose);
        
        // Lấy LinearLayout từ layout
        llSuggestionsList = dialog.findViewById(R.id.llSuggestionsList);
        
        // Setup click listeners
        ivClose.setOnClickListener(v -> dismiss());
        tvClose.setOnClickListener(v -> dismiss());
        btnRetry.setOnClickListener(v -> loadSuggestions());
        
        // Setup dialog window
        dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
        dialog.getWindow().setGravity(Gravity.BOTTOM);
    }
    
    /**
     * Hiển thị dialog và load suggestions
     */
    public void show() {
        if (dialog != null && !dialog.isShowing()) {
            dialog.show();
            loadSuggestions();
        }
    }
    
    /**
     * Load suggestions từ AI
     */
    private void loadSuggestions() {
        // Hiển thị loading
        showLoading();
        
        // Gọi AI helper
        suggestionHelper.getDrinkSuggestionsWithWeather(maKhachHang, new AISuggestionHelper.SuggestionCallback() {
            @Override
            public void onSuccess(String suggestion) {
                // Chạy trên main thread
                new Handler(Looper.getMainLooper()).post(() -> {
                    showContent(suggestion);
                });
            }
            
            @Override
            public void onError(String error) {
                // Chạy trên main thread
                new Handler(Looper.getMainLooper()).post(() -> {
                    showError(error);
                });
            }
        });
    }
    
    /**
     * Hiển thị loading state
     */
    private void showLoading() {
        llLoading.setVisibility(View.VISIBLE);
        cardContent.setVisibility(View.GONE);
        cardError.setVisibility(View.GONE);
    }
    
    /**
     * Hiển thị content với suggestion
     */
    private void showContent(String suggestion) {
        llLoading.setVisibility(View.GONE);
        cardContent.setVisibility(View.VISIBLE);
        cardError.setVisibility(View.GONE);
        
        // Format text (hỗ trợ HTML nếu có)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            tvSuggestion.setText(Html.fromHtml(formatSuggestion(suggestion), Html.FROM_HTML_MODE_LEGACY));
        } else {
            tvSuggestion.setText(Html.fromHtml(formatSuggestion(suggestion)));
        }
        
        // Parse và hiển thị danh sách món có thể thêm
        parseAndShowSuggestions(suggestion);
    }
    
    /**
     * Parse tên món từ suggestion và hiển thị danh sách có thể thêm
     */
    private void parseAndShowSuggestions(String suggestion) {
        // Xóa các item cũ
        llSuggestionsList.removeAllViews();
        
        // Tìm tất cả tên món trong dấu ngoặc kép
        Pattern pattern = Pattern.compile("\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(suggestion);
        
        List<String> suggestedDrinkNames = new ArrayList<>();
        while (matcher.find()) {
            String drinkName = matcher.group(1);
            suggestedDrinkNames.add(drinkName);
        }
        
        // Nếu không tìm thấy trong ngoặc kép, thử tìm sau số thứ tự
        if (suggestedDrinkNames.isEmpty()) {
            pattern = Pattern.compile("\\d+\\.\\s*([^\\-:]+?)(?:\\s*-|\\s*:|$)");
            matcher = pattern.matcher(suggestion);
            while (matcher.find()) {
                String drinkName = matcher.group(1).trim();
                // Loại bỏ các ký tự đặc biệt
                drinkName = drinkName.replaceAll("[^\\w\\s]", "").trim();
                if (!drinkName.isEmpty() && drinkName.length() > 2) {
                    suggestedDrinkNames.add(drinkName);
                }
            }
        }
        
        // Tìm món trong database và hiển thị
        ArrayList<HangHoa> allDrinks = hangHoaDAO.getAll();
        List<HangHoa> foundDrinks = new ArrayList<>();
        
        for (String drinkName : suggestedDrinkNames) {
            for (HangHoa hangHoa : allDrinks) {
                if (hangHoa.getTenHangHoa().toLowerCase().contains(drinkName.toLowerCase()) ||
                    drinkName.toLowerCase().contains(hangHoa.getTenHangHoa().toLowerCase())) {
                    if (hangHoa.getTrangThai() == HangHoa.STATUS_STILL && !foundDrinks.contains(hangHoa)) {
                        foundDrinks.add(hangHoa);
                        break;
                    }
                }
            }
        }
        
        // Hiển thị danh sách món
        if (!foundDrinks.isEmpty()) {
            llSuggestionsList.setVisibility(View.VISIBLE);
            
            TextView tvTitle = new TextView(context);
            tvTitle.setText("Thêm vào đơn:");tvTitle.setTextColor(context.getResources().getColor(R.color.BlackPrimary));
            tvTitle.setTextSize(16);
            tvTitle.setPadding(0, 0, 0, 12);
            llSuggestionsList.addView(tvTitle);
            
            for (HangHoa hangHoa : foundDrinks) {
                addDrinkItemView(hangHoa);
            }
        } else {
            llSuggestionsList.setVisibility(View.GONE);
        }
    }
    
    /**
     * Thêm view cho một món đồ uống
     */
    private void addDrinkItemView(HangHoa hangHoa) {
        // Tạo card view cho mỗi món
        CardView cardView = new CardView(context);
        cardView.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        cardView.setCardElevation(2);
        cardView.setRadius(8);
        cardView.setCardBackgroundColor(Color.WHITE);
        
        LinearLayout itemLayout = new LinearLayout(context);
        itemLayout.setOrientation(LinearLayout.HORIZONTAL);
        itemLayout.setPadding(16, 12, 16, 12);
        itemLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);
        
        // Tên món
        TextView tvDrinkName = new TextView(context);
        tvDrinkName.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1.0f));
        tvDrinkName.setText(hangHoa.getTenHangHoa());
        tvDrinkName.setTextColor(context.getResources().getColor(R.color.BlackPrimary));
        tvDrinkName.setTextSize(15);
        
        // Giá
        TextView tvPrice = new TextView(context);
        tvPrice.setText(hangHoa.getGiaTien() + " VND");
        tvPrice.setTextColor(context.getResources().getColor(R.color.BrowPrimary));
        tvPrice.setTextSize(14);
        tvPrice.setPadding(0, 0, 12, 0);
        
        // Nút thêm
        Button btnAdd = new Button(context);
        btnAdd.setText("Thêm");
        btnAdd.setBackground(context.getResources().getDrawable(R.drawable.bgr_btn_payment_primary));
        btnAdd.setTextColor(Color.WHITE);
        btnAdd.setTextSize(12);
        btnAdd.setPadding(16, 8, 16, 8);
        btnAdd.setAllCaps(false);
        btnAdd.setOnClickListener(v -> addDrinkToOrder(hangHoa));
        
        itemLayout.addView(tvDrinkName);
        itemLayout.addView(tvPrice);
        itemLayout.addView(btnAdd);
        
        cardView.addView(itemLayout);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 8);
        cardView.setLayoutParams(params);
        
        llSuggestionsList.addView(cardView);
    }
    
    /**
     * Thêm món vào đơn
     */
    private void addDrinkToOrder(HangHoa hangHoa) {
        if (maHoaDon == null || maHoaDon.isEmpty()) {
            MyToast.error(context, "Không tìm thấy hóa đơn");
            return;
        }
        
        try {
            HoaDonChiTiet hoaDonChiTiet = new HoaDonChiTiet();
            hoaDonChiTiet.setMaHoaDon(Integer.parseInt(maHoaDon));
            hoaDonChiTiet.setMaHangHoa(hangHoa.getMaHangHoa());
            hoaDonChiTiet.setSoLuong(1);
            hoaDonChiTiet.setGiaTien(hangHoa.getGiaTien() * hoaDonChiTiet.getSoLuong());
            Calendar calendar = Calendar.getInstance();
            hoaDonChiTiet.setNgayXuatHoaDon(calendar.getTime());
            
            if (hoaDonChiTietDAO.insertHoaDonChiTiet(hoaDonChiTiet)) {
                MyToast.successful(context, "Đã thêm " + hangHoa.getTenHangHoa() + " vào đơn");
                dismiss();
                
                // Refresh activity nếu là OderActivity
                if (context instanceof OderActivity) {
                    ((OderActivity) context).onResume();
                }
            } else {
                MyToast.error(context, "Thêm món thất bại");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error adding drink to order", e);
            MyToast.error(context, "Lỗi: " + e.getMessage());
        }
    }
    
    /**
     * Hiển thị error state
     */
    private void showError(String error) {
        llLoading.setVisibility(View.GONE);
        cardContent.setVisibility(View.GONE);
        cardError.setVisibility(View.VISIBLE);
        
        tvError.setText("Không thể tải gợi ý. " + error);
    }
    
    /**
     * Format suggestion text để hiển thị đẹp hơn
     */
    private String formatSuggestion(String suggestion) {
        // Thay thế các ký tự đặc biệt và format
        String formatted = suggestion
                .replace("\n\n", "<br><br>")
                .replace("\n", "<br>")
                .replace("**", "<b>")
                .replace("*", "<b>");
        
        // Đảm bảo các thẻ HTML được đóng đúng
        return formatted;
    }
    
    /**
     * Dismiss dialog
     */
    public void dismiss() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }
    
    /**
     * Check if dialog is showing
     */
    public boolean isShowing() {
        return dialog != null && dialog.isShowing();
    }
}

