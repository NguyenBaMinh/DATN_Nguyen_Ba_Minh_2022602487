package app.edu.app.service;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import app.edu.app.config.OpenAIConfig;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Service để gọi OpenAI API
 */
public class OpenAIService {
    private static final String TAG = "OpenAIService";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    
    private final OkHttpClient client;
    private final Gson gson;
    
    public OpenAIService() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        this.gson = new Gson();
    }
    
    /**
     * Gọi OpenAI API để lấy gợi ý
     * @param prompt Prompt để gửi cho AI
     * @param callback Callback để nhận kết quả
     */
    public void getSuggestion(String prompt, AISuggestionCallback callback) {
        getSuggestion(prompt, OpenAIConfig.DEFAULT_MODEL, callback);
    }
    
    /**
     * Gọi OpenAI API với model cụ thể
     */
    public void getSuggestion(String prompt, String model, AISuggestionCallback callback) {
        try {
            // Tạo request body
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("model", model);
            requestBody.addProperty("temperature", OpenAIConfig.DEFAULT_TEMPERATURE);
            requestBody.addProperty("max_tokens", OpenAIConfig.MAX_TOKENS);
            
            JsonArray messages = new JsonArray();
            JsonObject message = new JsonObject();
            message.addProperty("role", "user");
            message.addProperty("content", prompt);
            messages.add(message);
            
            requestBody.add("messages", messages);
            
            String jsonBody = gson.toJson(requestBody);
            Log.d(TAG, "Request body: " + jsonBody);
            
            RequestBody body = RequestBody.create(jsonBody, JSON);
            
            // Tạo request
            Request request = new Request.Builder()
                    .url(OpenAIConfig.API_URL)
                    .addHeader("Authorization", "Bearer " + OpenAIConfig.API_KEY)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();
            
            // Gọi API
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "API call failed", e);
                    if (callback != null) {
                        callback.onError("Lỗi kết nối: " + e.getMessage());
                    }
                }
                
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                        Log.e(TAG, "API error: " + response.code() + " - " + errorBody);
                        if (callback != null) {
                            callback.onError("Lỗi API: " + response.code() + " - " + errorBody);
                        }
                        return;
                    }
                    
                    try {
                        String responseBody = response.body().string();
                        Log.d(TAG, "Response: " + responseBody);
                        
                        JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
                        JsonArray choices = jsonResponse.getAsJsonArray("choices");
                        
                        if (choices != null && choices.size() > 0) {
                            JsonObject firstChoice = choices.get(0).getAsJsonObject();
                            JsonObject message = firstChoice.getAsJsonObject("message");
                            String content = message.get("content").getAsString();
                            
                            if (callback != null) {
                                callback.onSuccess(content);
                            }
                        } else {
                            if (callback != null) {
                                callback.onError("Không nhận được phản hồi từ AI");
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing response", e);
                        if (callback != null) {
                            callback.onError("Lỗi xử lý phản hồi: " + e.getMessage());
                        }
                    }
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "Error creating request", e);
            if (callback != null) {
                callback.onError("Lỗi tạo request: " + e.getMessage());
            }
        }
    }
    
    /**
     * Interface callback cho AI suggestion
     */
    public interface AISuggestionCallback {
        void onSuccess(String suggestion);
        void onError(String error);
    }
}

