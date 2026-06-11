package app.edu.app.service;

import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import app.edu.app.config.WeatherConfig;
import app.edu.app.model.WeatherInfo;

/**
 * Service gọi OpenWeatherMap API để lấy thông tin thời tiết hiện tại
 * API miễn phí: https://openweathermap.org/current
 *
 * Lưu ý: Phải chạy trên background thread (không gọi trên UI thread)
 */
public class WeatherService {

    private static final String TAG = "WeatherService";

    /**
     * Interface callback trả kết quả về UI thread
     */
    public interface WeatherCallback {
        void onSuccess(WeatherInfo weatherInfo);
        void onError(String error);
    }

    /**
     * Lấy thời tiết hiện tại tại thành phố đã cấu hình
     * Tự động chạy trên background thread
     */
    public void getCurrentWeather(WeatherCallback callback) {
        new Thread(() -> {
            try {
                WeatherInfo info = fetchWeather();
                if (callback != null) callback.onSuccess(info);
            } catch (Exception e) {
                Log.e(TAG, "Lỗi lấy thời tiết: " + e.getMessage());
                if (callback != null) callback.onError(e.getMessage());
            }
        }).start();
    }

    /**
     * Gọi API và parse JSON response
     */
    private WeatherInfo fetchWeather() throws Exception {
        // Xây dựng URL
        String urlStr = WeatherConfig.API_URL
                + "?q=" + WeatherConfig.CITY + "," + WeatherConfig.COUNTRY_CODE
                + "&appid=" + WeatherConfig.API_KEY
                + "&units=" + WeatherConfig.UNITS
                + "&lang=" + WeatherConfig.LANG;

        Log.d(TAG, "Gọi API: " + urlStr);

        // Kết nối HTTP
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(10000); // 10 giây
        conn.setReadTimeout(10000);

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            throw new Exception("API trả lỗi HTTP: " + responseCode);
        }

        // Đọc response
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line);
        reader.close();
        conn.disconnect();

        // Parse JSON
        return parseWeatherJson(sb.toString());
    }

    /**
     * Parse JSON từ OpenWeatherMap thành WeatherInfo
     *
     * Cấu trúc JSON trả về:
     * {
     *   "weather": [{ "id": 800, "main": "Clear", "description": "trời quang" }],
     *   "main": { "temp": 32.5, "feels_like": 36.0, "humidity": 78 },
     *   "wind": { "speed": 3.5 },
     *   "name": "Hanoi"
     * }
     */
    private WeatherInfo parseWeatherJson(String json) throws Exception {
        JSONObject root = new JSONObject(json);

        // Lấy mô tả thời tiết
        JSONObject weatherObj = root.getJSONArray("weather").getJSONObject(0);
        int weatherId      = weatherObj.getInt("id");
        String main        = weatherObj.getString("main");        // VD: "Rain", "Clear"
        String description = weatherObj.getString("description"); // VD: "mưa nhẹ"

        // Lấy nhiệt độ và độ ẩm
        JSONObject mainObj  = root.getJSONObject("main");
        double temp         = mainObj.getDouble("temp");
        double feelsLike    = mainObj.getDouble("feels_like");
        int humidity        = mainObj.getInt("humidity");

        // Lấy tốc độ gió
        double windSpeed    = root.getJSONObject("wind").getDouble("speed");

        // Tên thành phố
        String cityName     = root.getString("name");

        // Xác định loại thời tiết và gợi ý
        String weatherType  = classifyWeather(weatherId, temp, humidity);
        String suggestion   = getWeatherSuggestion(weatherType, temp);

        WeatherInfo info = new WeatherInfo();
        info.setCityName(cityName);
        info.setMain(main);
        info.setDescription(description);
        info.setTemp(temp);
        info.setFeelsLike(feelsLike);
        info.setHumidity(humidity);
        info.setWindSpeed(windSpeed);
        info.setWeatherType(weatherType);
        info.setWeatherSuggestion(suggestion);

        Log.d(TAG, "Thời tiết: " + weatherType + ", " + temp + "°C, " + description);
        return info;
    }

    /**
     * Phân loại thời tiết dựa trên Weather ID của OpenWeatherMap
     * Tham khảo: https://openweathermap.org/weather-conditions
     *
     * ID  200-299: Giông bão (Thunderstorm)
     * ID  300-399: Mưa phùn (Drizzle)
     * ID  500-599: Mưa (Rain)
     * ID  600-699: Tuyết (Snow) - hiếm ở VN
     * ID  700-799: Sương mù, bụi... (Atmosphere)
     * ID  800    : Trời quang (Clear)
     * ID  801-804: Có mây (Clouds)
     */
    private String classifyWeather(int weatherId, double temp, int humidity) {
        if (weatherId >= 200 && weatherId < 300) return "STORM";        // Giông bão
        if (weatherId >= 300 && weatherId < 400) return "DRIZZLE";      // Mưa phùn
        if (weatherId >= 500 && weatherId < 600) return "RAINY";        // Mưa
        if (weatherId >= 600 && weatherId < 700) return "COLD";         // Lạnh/Tuyết
        if (weatherId >= 700 && weatherId < 800) return "FOGGY";        // Sương mù
        if (weatherId == 800) {
            if (temp >= 35) return "HOT";                               // Nắng nóng
            if (temp >= 28) return "SUNNY_WARM";                        // Nắng ấm
            return "CLEAR";                                             // Trời quang mát
        }
        if (weatherId >= 801 && weatherId <= 804) {
            if (temp >= 30) return "CLOUDY_HOT";                        // Mây nóng
            return "CLOUDY";                                            // Nhiều mây
        }
        return "NORMAL";
    }

    /**
     * Gợi ý loại đồ uống phù hợp với thời tiết
     * (Dùng để bổ sung context cho GPT)
     */
    private String getWeatherSuggestion(String weatherType, double temp) {
        switch (weatherType) {
            case "HOT":
                return "Trời nắng nóng trên 35°C, nên ưu tiên đồ uống lạnh, đá, giải khát mạnh";
            case "SUNNY_WARM":
                return "Trời nắng ấm khoảng " + (int)temp + "°C, phù hợp cả đồ uống lạnh và nóng";
            case "RAINY":
            case "DRIZZLE":
                return "Trời mưa, khách hàng thường thích đồ uống nóng ấm, ngọt ngào";
            case "STORM":
                return "Trời giông bão, nên gợi ý đồ uống nóng ấm, comfort drinks";
            case "CLOUDY_HOT":
                return "Trời nhiều mây nhưng nóng " + (int)temp + "°C, phù hợp đồ uống mát lạnh";
            case "CLOUDY":
                return "Trời nhiều mây mát mẻ, phù hợp cả đồ uống nóng và lạnh";
            case "FOGGY":
                return "Trời sương mù, se lạnh, nên gợi ý đồ uống nóng ấm";
            case "COLD":
                return "Thời tiết lạnh dưới " + (int)temp + "°C, ưu tiên đồ uống nóng";
            case "CLEAR":
                return "Trời quang mát mẻ khoảng " + (int)temp + "°C, phù hợp mọi loại đồ uống";
            default:
                return "Thời tiết bình thường " + (int)temp + "°C";
        }
    }
}
