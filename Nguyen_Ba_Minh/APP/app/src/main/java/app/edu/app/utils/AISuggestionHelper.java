package app.edu.app.utils;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import app.edu.app.dao.HangHoaDAO;
import app.edu.app.dao.HoaDonChiTietDAO;
import app.edu.app.dao.HoaDonDAO;
import app.edu.app.model.HangHoa;
import app.edu.app.model.HoaDon;
import app.edu.app.model.HoaDonChiTiet;
import app.edu.app.model.WeatherInfo;
import app.edu.app.service.GeminiService;
import app.edu.app.service.WeatherService;

/**
 * Helper class phân tích lịch sử đặt hàng + thời tiết
 * và tạo gợi ý đồ uống bằng Gemini AI
 */
public class AISuggestionHelper {

    private static final String TAG = "AISuggestionHelper";

    private final Context context;
    private final HoaDonDAO hoaDonDAO;
    private final HoaDonChiTietDAO hoaDonChiTietDAO;
    private final HangHoaDAO hangHoaDAO;
    private final GeminiService geminiService;
    private final WeatherService weatherService;

    public AISuggestionHelper(Context context) {
        this.context = context;
        this.hoaDonDAO = new HoaDonDAO(context);
        this.hoaDonChiTietDAO = new HoaDonChiTietDAO(context);
        this.hangHoaDAO = new HangHoaDAO(context);
        this.geminiService = new GeminiService();
        this.weatherService = new WeatherService();
    }

    // =========================================================
    // HELPER: thay thế getOrDefault() cho API 21
    // =========================================================

    private int getOrDefault(Map<String, Integer> map, String key) {
        Integer value = map.get(key);
        return value != null ? value : 0;
    }

    // =========================================================
    // ENTRY POINT CHÍNH
    // =========================================================

    public void getDrinkSuggestionsWithWeather(String maKhachHang, SuggestionCallback callback) {
        Log.d(TAG, "Bắt đầu lấy gợi ý cho khách: " + maKhachHang);

        weatherService.getCurrentWeather(new WeatherService.WeatherCallback() {
            @Override
            public void onSuccess(WeatherInfo weatherInfo) {
                Log.d(TAG, "Lấy thời tiết thành công: " + weatherInfo.toPromptString());
                buildSuggestionWithWeather(maKhachHang, weatherInfo, callback);
            }

            @Override
            public void onError(String error) {
                Log.w(TAG, "Không lấy được thời tiết: " + error + " → fallback lịch sử");
                getDrinkSuggestionsFromHistoryOnly(maKhachHang, callback);
            }
        });
    }

    // =========================================================
    // BƯỚC 2 & 3: LẤY LỊCH SỬ VÀ BUILD PROMPT
    // =========================================================

    private void buildSuggestionWithWeather(String maKhachHang,
                                            WeatherInfo weatherInfo,
                                            SuggestionCallback callback) {
        ArrayList<HoaDon> hoaDons = hoaDonDAO.getByMaKhachHang(maKhachHang);
        List<HoaDon> paidOrders = new ArrayList<>();
        for (HoaDon hd : hoaDons) {
            if (hd.getTrangThai() == HoaDon.DA_THANH_TOAN) paidOrders.add(hd);
        }

        String prompt;
        if (paidOrders.isEmpty()) {
            prompt = buildPromptWeatherOnly(weatherInfo);
        } else {
            prompt = buildPromptWeatherAndHistory(weatherInfo, paidOrders);
        }

        Log.d(TAG, "Prompt đã tạo:\n" + prompt);

        geminiService.getSuggestion(prompt, new GeminiService.AISuggestionCallback() {
            @Override
            public void onSuccess(String suggestion) {
                if (callback != null) callback.onSuccess(suggestion);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Gemini lỗi: " + error);
                if (callback != null) callback.onError(error);
            }
        });
    }

    // =========================================================
    // BUILD PROMPT: THỜI TIẾT + LỊCH SỬ
    // =========================================================

    private String buildPromptWeatherAndHistory(WeatherInfo weather, List<HoaDon> paidOrders) {

        Map<String, Integer> drinkFrequency   = new HashMap<>();
        Map<String, Integer> drinkTotalQty    = new HashMap<>();
        Map<String, Map<String, Integer>> byDay  = new HashMap<>();
        Map<String, Map<String, Integer>> byTime = new HashMap<>();

        Calendar calendar = Calendar.getInstance();
        Calendar now = Calendar.getInstance();
        String currentDay  = getDayName(now.get(Calendar.DAY_OF_WEEK));
        String currentTime = getTimePeriod(now.get(Calendar.HOUR_OF_DAY));

        for (HoaDon hd : paidOrders) {
            if (hd.getGioVao() == null) continue;
            calendar.setTime(hd.getGioVao());
            String dayName    = getDayName(calendar.get(Calendar.DAY_OF_WEEK));
            String timePeriod = getTimePeriod(calendar.get(Calendar.HOUR_OF_DAY));

            ArrayList<HoaDonChiTiet> chiTiets =
                    hoaDonChiTietDAO.getByMaHoaDon(String.valueOf(hd.getMaHoaDon()));

            for (HoaDonChiTiet ct : chiTiets) {
                HangHoa hh = hangHoaDAO.getByMaHangHoa(String.valueOf(ct.getMaHangHoa()));
                if (hh == null) continue;
                String ten = hh.getTenHangHoa();

                // ✅ Dùng helper thay getOrDefault() — tương thích API 21
                drinkFrequency.put(ten, getOrDefault(drinkFrequency, ten) + 1);
                drinkTotalQty.put(ten, getOrDefault(drinkTotalQty, ten) + ct.getSoLuong());

                if (!byDay.containsKey(ten)) byDay.put(ten, new HashMap<>());
                Map<String, Integer> dm = byDay.get(ten);
                dm.put(dayName, getOrDefault(dm, dayName) + 1);

                if (!byTime.containsKey(ten)) byTime.put(ten, new HashMap<>());
                Map<String, Integer> tm = byTime.get(ten);
                tm.put(timePeriod, getOrDefault(tm, timePeriod) + 1);
            }
        }

        StringBuilder history = new StringBuilder();
        history.append("LỊCH SỬ ĐẶT HÀNG:\n");
        for (Map.Entry<String, Integer> e : drinkFrequency.entrySet()) {
            history.append("- ").append(e.getKey())
                    .append(": ").append(e.getValue()).append(" lần, ")
                    .append("tổng ").append(getOrDefault(drinkTotalQty, e.getKey()))
                    .append(" ly\n");
        }

        history.append("\nTHÓI QUEN THEO NGÀY:\n");
        for (Map.Entry<String, Map<String, Integer>> e : byDay.entrySet()) {
            history.append("- ").append(e.getKey()).append(": ");
            List<String> parts = new ArrayList<>();
            for (Map.Entry<String, Integer> d : e.getValue().entrySet())
                parts.add(d.getKey() + " (" + d.getValue() + " lần)");
            history.append(android.text.TextUtils.join(", ", parts)).append("\n");
        }

        history.append("\nTHÓI QUEN THEO BUỔI:\n");
        for (Map.Entry<String, Map<String, Integer>> e : byTime.entrySet()) {
            history.append("- ").append(e.getKey()).append(": ");
            List<String> parts = new ArrayList<>();
            for (Map.Entry<String, Integer> t : e.getValue().entrySet())
                parts.add(t.getKey() + " (" + t.getValue() + " lần)");
            history.append(android.text.TextUtils.join(", ", parts)).append("\n");
        }

        String menu = buildMenuString();

        return "Bạn là chuyên gia tư vấn đồ uống tại quán cà phê tại Hà Nội.\n\n"
                + "=== THỜI TIẾT HIỆN TẠI ===\n"
                + weather.toPromptString() + "\n"
                + "Nhận xét thời tiết: " + weather.getWeatherSuggestion() + "\n\n"
                + "=== " + history.toString() + "\n"
                + "=== THÔNG TIN HIỆN TẠI ===\n"
                + "- Hôm nay: " + currentDay + "\n"
                + "- Buổi: " + currentTime + "\n\n"
                + menu + "\n\n"
                + "=== YÊU CẦU ===\n"
                + "1. Ưu tiên gợi ý phù hợp với thời tiết hiện tại (" + weather.getDescription() + ", "
                + String.format("%.0f", weather.getTemp()) + "°C)\n"
                + "2. Kết hợp với thói quen của khách vào " + currentDay + " buổi " + currentTime + "\n"
                + "3. Gợi ý 3-5 đồ uống, mỗi gợi ý giải thích lý do (cả thời tiết lẫn sở thích)\n"
                + "4. Trả lời tiếng Việt, thân thiện, không quá 250 từ\n"
                + "5. Tên đồ uống CHÍNH XÁC với menu, đặt trong dấu ngoặc kép \"\"\n"
                + "6. Bắt đầu response bằng 1 dòng tóm tắt thời tiết: VD: Hà Nội - 32°C, trời nắng ấm\n"
                + "7. Format gợi ý: 1. \"Tên đồ uống\" - Lý do (đề cập thời tiết + thói quen)";
    }

    private String buildPromptWeatherOnly(WeatherInfo weather) {
        String menu = buildMenuString();
        Calendar now = Calendar.getInstance();

        return "Bạn là chuyên gia tư vấn đồ uống tại quán cà phê tại Hà Nội.\n\n"
                + "=== THỜI TIẾT HIỆN TẠI ===\n"
                + weather.toPromptString() + "\n"
                + "Nhận xét thời tiết: " + weather.getWeatherSuggestion() + "\n\n"
                + "=== THÔNG TIN HIỆN TẠI ===\n"
                + "- Hôm nay: " + getDayName(now.get(Calendar.DAY_OF_WEEK)) + "\n"
                + "- Buổi: " + getTimePeriod(now.get(Calendar.HOUR_OF_DAY)) + "\n\n"
                + menu + "\n\n"
                + "=== YÊU CẦU ===\n"
                + "1. Gợi ý 3-5 đồ uống phù hợp nhất với thời tiết (" + weather.getDescription()
                + ", " + String.format("%.0f", weather.getTemp()) + "°C)\n"
                + "2. Đây là khách mới, chưa có lịch sử đặt hàng\n"
                + "3. Giải thích tại sao đồ uống phù hợp với thời tiết hôm nay\n"
                + "4. Trả lời tiếng Việt, thân thiện, không quá 200 từ\n"
                + "5. Tên đồ uống CHÍNH XÁC với menu, đặt trong dấu ngoặc kép \"\"\n"
                + "6. Bắt đầu response bằng 1 dòng tóm tắt thời tiết: VD: Hà Nội - 32°C, trời nắng ấm\n"
                + "7. Format gợi ý: 1. \"Tên đồ uống\" - Lý do";
    }

    // =========================================================
    // FALLBACK: CHỈ LỊCH SỬ
    // =========================================================

    public void getDrinkSuggestionsFromHistoryOnly(String maKhachHang, SuggestionCallback callback) {
        ArrayList<HoaDon> hoaDons = hoaDonDAO.getByMaKhachHang(maKhachHang);
        List<HoaDon> paidOrders = new ArrayList<>();
        for (HoaDon hd : hoaDons) {
            if (hd.getTrangThai() == HoaDon.DA_THANH_TOAN) paidOrders.add(hd);
        }

        String prompt = paidOrders.isEmpty()
                ? buildPromptMenuOnly()
                : buildPromptHistoryOnly(paidOrders);

        geminiService.getSuggestion(prompt, new GeminiService.AISuggestionCallback() {
            @Override
            public void onSuccess(String s) { if (callback != null) callback.onSuccess(s); }
            @Override
            public void onError(String e) { if (callback != null) callback.onError(e); }
        });
    }

    private String buildPromptHistoryOnly(List<HoaDon> paidOrders) {
        Map<String, Integer> drinkFrequency = new HashMap<>();
        Calendar now = Calendar.getInstance();
        String currentDay  = getDayName(now.get(Calendar.DAY_OF_WEEK));
        String currentTime = getTimePeriod(now.get(Calendar.HOUR_OF_DAY));

        for (HoaDon hd : paidOrders) {
            if (hd.getGioVao() == null) continue;
            ArrayList<HoaDonChiTiet> chiTiets =
                    hoaDonChiTietDAO.getByMaHoaDon(String.valueOf(hd.getMaHoaDon()));
            for (HoaDonChiTiet ct : chiTiets) {
                HangHoa hh = hangHoaDAO.getByMaHangHoa(String.valueOf(ct.getMaHangHoa()));
                if (hh != null)
                    // ✅ Dùng helper thay getOrDefault()
                    drinkFrequency.put(hh.getTenHangHoa(),
                            getOrDefault(drinkFrequency, hh.getTenHangHoa()) + 1);
            }
        }

        StringBuilder sb = new StringBuilder("LỊCH SỬ:\n");
        for (Map.Entry<String, Integer> e : drinkFrequency.entrySet())
            sb.append("- ").append(e.getKey()).append(": ").append(e.getValue()).append(" lần\n");

        return "Bạn là chuyên gia tư vấn đồ uống. Lưu ý: không lấy được thời tiết hôm nay.\n\n"
                + sb + "\n" + buildMenuString() + "\n\n"
                + "Hôm nay là " + currentDay + " buổi " + currentTime + ".\n"
                + "Gợi ý 3-5 đồ uống phù hợp với thói quen, trả lời tiếng Việt ngắn gọn.\n"
                + "Tên đồ uống trong dấu ngoặc kép \"\".";
    }

    private String buildPromptMenuOnly() {
        return "Bạn là chuyên gia tư vấn đồ uống. Khách mới, chưa có lịch sử.\n\n"
                + buildMenuString() + "\n\n"
                + "Gợi ý 3-5 đồ uống phổ biến nhất, trả lời tiếng Việt ngắn gọn.\n"
                + "Tên đồ uống trong dấu ngoặc kép \"\".";
    }

    // =========================================================
    // HELPER
    // =========================================================

    private String buildMenuString() {
        ArrayList<HangHoa> allDrinks = hangHoaDAO.getAll();
        StringBuilder menu = new StringBuilder("=== MENU HIỆN TẠI ===\n");
        for (HangHoa hh : allDrinks) {
            if (hh.getTrangThai() == HangHoa.STATUS_STILL) {
                menu.append("- \"").append(hh.getTenHangHoa())
                        .append("\" (").append(hh.getGiaTien()).append(" VND)\n");
            }
        }
        return menu.toString();
    }

    private String getDayName(int dayOfWeek) {
        switch (dayOfWeek) {
            case Calendar.MONDAY:    return "Thu Hai";
            case Calendar.TUESDAY:   return "Thu Ba";
            case Calendar.WEDNESDAY: return "Thu Tu";
            case Calendar.THURSDAY:  return "Thu Nam";
            case Calendar.FRIDAY:    return "Thu Sau";
            case Calendar.SATURDAY:  return "Thu Bay";
            case Calendar.SUNDAY:    return "Chu Nhat";
            default:                 return "Khong xac dinh";
        }
    }

    private String getTimePeriod(int hour) {
        if (hour >= 5  && hour < 11) return "Sang";
        if (hour >= 11 && hour < 14) return "Trua";
        if (hour >= 14 && hour < 18) return "Chieu";
        if (hour >= 18 && hour < 22) return "Toi";
        return "Dem";
    }

    // =========================================================
    // INTERFACE CALLBACK
    // =========================================================

    public interface SuggestionCallback {
        void onSuccess(String suggestion);
        void onError(String error);
    }
}