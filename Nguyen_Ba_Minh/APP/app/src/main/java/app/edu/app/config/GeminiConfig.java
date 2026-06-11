package app.edu.app.config;

/**
 * Google Gemini API Configuration
 * Thay thế OpenAI API - Miễn phí 15 request/phút
 * Lấy API key tại: https://aistudio.google.com
 */
public class GeminiConfig {


    public static final String API_KEY = "AIzaSyBiK6POr_F8l1sTUU8xaTz1pbpD7_wCFrs";

    // Model miễn phí tốt nhất hiện tại
    public static final String MODEL = "gemini-2.5-flash-lite";

    // Endpoint
    public static final String API_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/"
                    + MODEL + ":generateContent?key=" + API_KEY;

    // Giới hạn token response
    public static final int MAX_TOKENS = 500;

    // Temperature (0.0 - 1.0)
    public static final double TEMPERATURE = 0.7;
}