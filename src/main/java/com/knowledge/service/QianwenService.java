package com.knowledge.service;

import com.google.gson.*;
import com.knowledge.config.QianwenConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class QianwenService {
    
    private final QianwenConfig qianwenConfig;
    private final OkHttpClient httpClient;
    private final Gson gson = new Gson();
    
    public QianwenService(QianwenConfig qianwenConfig) {
        this.qianwenConfig = qianwenConfig;
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(qianwenConfig.getTimeout(), TimeUnit.MILLISECONDS)
            .readTimeout(qianwenConfig.getTimeout(), TimeUnit.MILLISECONDS)
            .writeTimeout(qianwenConfig.getTimeout(), TimeUnit.MILLISECONDS)
            .build();
    }
    
    public float[] getEmbedding(String text) {
        try {
            log.info("Getting embedding for text: {}", text.substring(0, Math.min(50, text.length())));
            
            // 通义千问 Embedding API 格式 - 使用 input.texts 数组
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("model", qianwenConfig.getEmbeddingModel());
            
            // input 对象包含 texts 数组
            JsonObject inputObj = new JsonObject();
            JsonArray textsArray = new JsonArray();
            textsArray.add(text);
            inputObj.add("texts", textsArray);
            requestBody.add("input", inputObj);
            
            String jsonBody = gson.toJson(requestBody);
            log.info("Embedding request body: {}", jsonBody);
            
            RequestBody body = RequestBody.create(
                jsonBody,
                MediaType.parse("application/json")
            );
            
            Request request = new Request.Builder()
                .url(qianwenConfig.getEmbeddingApiUrl())
                .post(body)
                .addHeader("Authorization", "Bearer " + qianwenConfig.getApiKey())
                .addHeader("Content-Type", "application/json")
                .build();
            
            log.info("Calling embedding API: {}", qianwenConfig.getEmbeddingApiUrl());
            log.info("API Key prefix: {}", qianwenConfig.getApiKey().substring(0, Math.min(10, qianwenConfig.getApiKey().length())) + "...");
            
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "No response body";
                    log.error("Embedding API call failed with code {}: {}", response.code(), errorBody);
                    throw new RuntimeException("API call failed: " + response.code() + " - " + errorBody);
                }
                
                String responseBody = response.body().string();
                JsonObject jsonResponse = JsonParser.parseString(responseBody).getAsJsonObject();
                JsonArray embeddings = jsonResponse.getAsJsonObject("output")
                    .getAsJsonArray("embeddings");
                
                JsonArray values = embeddings.get(0).getAsJsonObject()
                    .getAsJsonArray("embedding");
                
                float[] result = new float[values.size()];
                for (int i = 0; i < values.size(); i++) {
                    result[i] = values.get(i).getAsFloat();
                }
                
                return result;
            }
        } catch (IOException e) {
            log.error("Failed to get embedding: {}", e.getMessage());
            throw new RuntimeException("Failed to get embedding", e);
        }
    }
    
    public String chat(String prompt, List<Map<String, String>> history) {
        try {
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("model", qianwenConfig.getModel());
            
            JsonArray messagesArray = new JsonArray();
            
            // Add history if exists
            if (history != null && !history.isEmpty()) {
                for (Map<String, String> msg : history) {
                    JsonObject userMsg = new JsonObject();
                    userMsg.addProperty("role", "user");
                    userMsg.addProperty("content", msg.get("question"));
                    messagesArray.add(userMsg);
                    
                    JsonObject assistantMsg = new JsonObject();
                    assistantMsg.addProperty("role", "assistant");
                    assistantMsg.addProperty("content", msg.get("answer"));
                    messagesArray.add(assistantMsg);
                }
            }
            
            // Add current prompt
            JsonObject currentMsg = new JsonObject();
            currentMsg.addProperty("role", "user");
            currentMsg.addProperty("content", prompt);
            messagesArray.add(currentMsg);
            
            JsonObject input = new JsonObject();
            input.add("messages", messagesArray);
            requestBody.add("input", input);
            
            JsonObject parameters = new JsonObject();
            parameters.addProperty("result_format", "message");
            requestBody.add("parameters", parameters);
            
            RequestBody body = RequestBody.create(
                gson.toJson(requestBody),
                MediaType.parse("application/json")
            );
            
            Request request = new Request.Builder()
                .url(qianwenConfig.getChatApiUrl())
                .post(body)
                .addHeader("Authorization", "Bearer " + qianwenConfig.getApiKey())
                .addHeader("Content-Type", "application/json")
                .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new RuntimeException("API call failed: " + response.code());
                }
                
                String responseBody = response.body().string();
                JsonObject jsonResponse = JsonParser.parseString(responseBody).getAsJsonObject();
                
                return jsonResponse.getAsJsonObject("output")
                    .getAsJsonArray("choices")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("message")
                    .get("content").getAsString();
            }
        } catch (IOException e) {
            log.error("Failed to chat: {}", e.getMessage());
            throw new RuntimeException("Failed to chat", e);
        }
    }
}
