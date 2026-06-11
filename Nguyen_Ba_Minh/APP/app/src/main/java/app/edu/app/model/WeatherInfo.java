package app.edu.app.model;

/**
 * Model lưu thông tin thời tiết từ OpenWeatherMap
 */
public class WeatherInfo {

    private String cityName;        // Tên thành phố
    private String main;            // VD: "Rain", "Clear", "Clouds"
    private String description;     // VD: "mưa nhẹ", "trời quang"
    private double temp;            // Nhiệt độ thực (°C)
    private double feelsLike;       // Nhiệt độ cảm giác (°C)
    private int    humidity;        // Độ ẩm (%)
    private double windSpeed;       // Tốc độ gió (m/s)
    private String weatherType;     // Loại thời tiết đã phân loại: HOT, RAINY, CLOUDY...
    private String weatherSuggestion; // Gợi ý loại đồ uống theo thời tiết

    // ===== Getters & Setters =====

    public String getCityName() { return cityName; }
    public void setCityName(String cityName) { this.cityName = cityName; }

    public String getMain() { return main; }
    public void setMain(String main) { this.main = main; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getTemp() { return temp; }
    public void setTemp(double temp) { this.temp = temp; }

    public double getFeelsLike() { return feelsLike; }
    public void setFeelsLike(double feelsLike) { this.feelsLike = feelsLike; }

    public int getHumidity() { return humidity; }
    public void setHumidity(int humidity) { this.humidity = humidity; }

    public double getWindSpeed() { return windSpeed; }
    public void setWindSpeed(double windSpeed) { this.windSpeed = windSpeed; }

    public String getWeatherType() { return weatherType; }
    public void setWeatherType(String weatherType) { this.weatherType = weatherType; }

    public String getWeatherSuggestion() { return weatherSuggestion; }
    public void setWeatherSuggestion(String weatherSuggestion) { this.weatherSuggestion = weatherSuggestion; }

    /**
     * Tóm tắt thời tiết dạng chuỗi để đưa vào prompt GPT
     */
    public String toPromptString() {
        return String.format(
                "Thành phố: %s | Thời tiết: %s (%s) | Nhiệt độ: %.1f°C (cảm giác %.1f°C) | Độ ẩm: %d%% | Gió: %.1f m/s",
                cityName, description, main, temp, feelsLike, humidity, windSpeed
        );
    }
}
