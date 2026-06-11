package app.edu.app.service;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import app.edu.app.config.GeminiConfig;

/**
 * Service gọi Google Gemini API
 * Thay thế OpenAIService - Miễn phí 15 request/phút
 *
 * Khác biệt với OpenAI:
 * - Endpoint khác: generativelanguage.googleapis.com
 * - Format JSON request/response khác
 * - Xác thực qua query param ?key= thay vì header Authorization
 */
public class GeminiService {

    private static final String TAG = "GeminiService";

    /**
     * Interface callback
     */
    public interface AISuggestionCallback {
        void onSuccess(String suggestion);
        void onError(String error);
    }

    /**
     * Gửi prompt lên Gemini và nhận gợi ý
     * Tự động chạy trên background thread
     */
    public void getSuggestion(String prompt, AISuggestionCallback callback) {
        new Thread(() -> {
            try {
                String result = callGeminiAPI(prompt);
                if (callback != null) callback.onSuccess(result);
            } catch (Exception e) {
                Log.e(TAG, "Lỗi gọi Gemini API: " + e.getMessage());
                if (callback != null) callback.onError(e.getMessage());
            }
        }).start();
    }

    /**
     * Gọi Gemini API và parse response
     *
     * Request format:
     * {
     *   "contents": [{ "parts": [{ "text": "prompt" }] }],
     *   "generationConfig": { "maxOutputTokens": 500, "temperature": 0.7 }
     * }
     *
     * Response format:
     * {
     *   "candidates": [{
     *     "content": { "parts": [{ "text": "response" }] }
     *   }]
     * }
     */
    private String callGeminiAPI(String prompt) throws Exception {
        URL url = new URL(GeminiConfig.API_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(30000);

        // Build request body
        JSONObject requestBody = buildRequestBody(prompt);
        Log.d(TAG, "Gửi request lên Gemini...");

        // Gửi request
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = requestBody.toString().getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int responseCode = conn.getResponseCode();
        Log.d(TAG, "Response code: " + responseCode);

        // Đọc response
        BufferedReader reader;
        if (responseCode == 200) {
            reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        } else {
            reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
            StringBuilder errorSb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) errorSb.append(line);
            reader.close();
            throw new Exception("Gemini API lỗi " + responseCode + ": " + errorSb);
        }

        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line);
        reader.close();
        conn.disconnect();

        // Parse response
        return parseResponse(sb.toString());
    }

    /**
     * Build JSON request body cho Gemini
     */
    private JSONObject buildRequestBody(String prompt) throws Exception {
        JSONObject requestBody = new JSONObject();

        // Contents array
        JSONArray contents = new JSONArray();
        JSONObject content = new JSONObject();
        JSONArray parts = new JSONArray();
        JSONObject part = new JSONObject();
        part.put("text", prompt);
        parts.put(part);
        content.put("parts", parts);
        contents.put(content);
        requestBody.put("contents", contents);

        // Generation config
        JSONObject generationConfig = new JSONObject();
        generationConfig.put("maxOutputTokens", GeminiConfig.MAX_TOKENS);
        generationConfig.put("temperature", GeminiConfig.TEMPERATURE);
        requestBody.put("generationConfig", generationConfig);

        return requestBody;
    }

    /**
     * Parse JSON response từ Gemini
     */
    private String parseResponse(String jsonResponse) throws Exception {
        JSONObject root = new JSONObject(jsonResponse);

        // Lấy text từ candidates[0].content.parts[0].text
        JSONArray candidates = root.getJSONArray("candidates");
        if (candidates.length() == 0) {
            throw new Exception("Gemini không trả về kết quả");
        }

        JSONObject candidate = candidates.getJSONObject(0);
        JSONObject contentObj = candidate.getJSONObject("content");
        JSONArray parts = contentObj.getJSONArray("parts");
        String text = parts.getJSONObject(0).getString("text");

        Log.d(TAG, "Gemini trả về: " + text.substring(0, Math.min(100, text.length())) + "...");
        return text;
    }
}
