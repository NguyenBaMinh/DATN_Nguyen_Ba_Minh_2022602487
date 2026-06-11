package app.edu.app.config;

/**
 * OpenWeatherMap Configuration
 * Đăng ký API key miễn phí tại: https://openweathermap.org/api
 */
public class WeatherConfig {

    // API Key - Đăng ký miễn phí tại openweathermap.org
    // Sau khi đăng ký, vào My API Keys để lấy key
    public static final String API_KEY = "081a1cf36c722360f4aef7c1dba0d2cd";

    // Endpoint lấy thời tiết hiện tại
    public static final String API_URL =
            "https://api.openweathermap.org/data/2.5/weather";

    // Thành phố cố định
    public static final String CITY = "Hanoi";
    public static final String COUNTRY_CODE = "VN";

    // Đơn vị nhiệt độ: metric = Celsius
    public static final String UNITS = "metric";

    // Ngôn ngữ trả về (vi = tiếng Việt)
    public static final String LANG = "vi";
}
