package com.knowledge.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.knowledge.config.QdrantConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class QdrantService {
    
    private final QdrantConfig qdrantConfig;
    private OkHttpClient httpClient;
    private String baseUrl;
    private final Gson gson = new Gson();
    private boolean initialized = false;
    
    @PostConstruct
    public void init() {
        try {
            baseUrl = String.format("http://%s:%d", qdrantConfig.getHost(), qdrantConfig.getPort());
            log.info("Connecting to Qdrant REST API at {}", baseUrl);
            
            httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
            
            // 测试连接
            testConnection();
            
            initialized = true;
            log.info("Qdrant client initialized successfully");
        } catch (Exception e) {
            log.error("Failed to initialize Qdrant client: {}", e.getMessage(), e);
            log.error("Please make sure Qdrant is running at {}", baseUrl);
            log.warn("Application will start but Qdrant features may not work");
            initialized = false;
        }
    }
    
    private void testConnection() throws IOException {
        Request request = new Request.Builder()
            .url(baseUrl + "/")
            .get()
            .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected response: " + response);
            }
            String body = response.body().string();
            log.info("Qdrant connection test successful: {}", body);
        }
    }
    
    public void createCollection() {
        try {
            JsonObject requestBody = new JsonObject();
            JsonObject vectorParams = new JsonObject();
            
            vectorParams.addProperty("size", qdrantConfig.getVectorSize());
            vectorParams.addProperty("distance", qdrantConfig.getDistance());
            // Qdrant REST API: vectors 直接是参数对象，不需要额外嵌套
            requestBody.add("vectors", vectorParams);
            
            String json = gson.toJson(requestBody);
            RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));
            
            Request request = new Request.Builder()
                .url(baseUrl + "/collections/" + qdrantConfig.getCollectionName())
                .put(body)
                .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    log.info("Collection created: {}", qdrantConfig.getCollectionName());
                } else {
                    log.warn("Collection creation response: {}", response.code());
                }
            }
        } catch (Exception e) {
            log.warn("Collection may already exist or creation failed: {}", e.getMessage());
        }
    }
    
    public void upsertPoints(String documentId, List<float[]> vectors, List<Map<String, Object>> payloads) {
        try {
            JsonObject requestBody = new JsonObject();
            JsonArray points = new JsonArray();
            
            for (int i = 0; i < vectors.size(); i++) {
                JsonObject point = new JsonObject();
                // Qdrant要求ID必须是数字或UUID，使用长整型ID
                long pointId = Long.parseLong(documentId) * 1000 + i;
                point.addProperty("id", pointId);
                
                JsonArray vectorArray = new JsonArray();
                for (float v : vectors.get(i)) {
                    vectorArray.add(v);
                }
                point.add("vector", vectorArray);
                
                JsonObject payload = new JsonObject();
                Map<String, Object> p = payloads.get(i);
                for (Map.Entry<String, Object> entry : p.entrySet()) {
                    if (entry.getValue() instanceof String) {
                        payload.addProperty(entry.getKey(), (String) entry.getValue());
                    } else if (entry.getValue() instanceof Number) {
                        payload.addProperty(entry.getKey(), (Number) entry.getValue());
                    }
                }
                point.add("payload", payload);
                points.add(point);
            }
            
            requestBody.add("points", points);
            
            String json = gson.toJson(requestBody);
            log.info("Upsert request body (first 200 chars): {}", json.length() > 200 ? json.substring(0, 200) + "..." : json);
            
            RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));
            
            // Qdrant REST API: upsert需要添加wait=true参数
            Request request = new Request.Builder()
                .url(baseUrl + "/collections/" + qdrantConfig.getCollectionName() + "/points?wait=true")
                .put(body)
                .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "No response body";
                log.info("Upsert response code: {}, body: {}", response.code(), responseBody);
                
                if (response.isSuccessful()) {
                    log.info("Upserted {} points for document {}", vectors.size(), documentId);
                } else {
                    throw new IOException("Failed to upsert: " + response.code() + " - " + responseBody);
                }
            }
        } catch (Exception e) {
            log.error("Failed to upsert points: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to upsert points", e);
        }
    }
    
    public List<Map<String, Object>> searchSimilar(float[] vector, int topK) {
        try {
            JsonObject requestBody = new JsonObject();
            
            // Qdrant REST API: vector 直接是数组
            JsonArray vectorArray = new JsonArray();
            for (float v : vector) {
                vectorArray.add(v);
            }
            requestBody.add("vector", vectorArray);
            requestBody.addProperty("limit", topK);
            
            // with_payload 使用布尔值
            requestBody.addProperty("with_payload", true);
            requestBody.addProperty("with_vector", false);
            
            String json = gson.toJson(requestBody);
            log.info("Search request body: {}", json);
            
            RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));
            
            Request request = new Request.Builder()
                .url(baseUrl + "/collections/" + qdrantConfig.getCollectionName() + "/points/search")
                .post(body)
                .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "No response body";
                log.debug("Search response code: {}, body length: {}", response.code(), responseBody.length());
                
                if (response.isSuccessful()) {
                    JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
                    
                    // Qdrant REST API: result 字段直接是数组
                    JsonElement resultElement = jsonResponse.get("result");
                    JsonArray scoredPoints;
                    
                    if (resultElement.isJsonArray()) {
                        // result 直接是数组
                        scoredPoints = resultElement.getAsJsonArray();
                    } else if (resultElement.isJsonObject()) {
                        // result 是对象，包含 points 数组
                        JsonObject resultObj = resultElement.getAsJsonObject();
                        scoredPoints = resultObj.getAsJsonArray("points");
                    } else {
                        log.warn("Unexpected result type: {}", resultElement.getClass().getSimpleName());
                        return new ArrayList<>();
                    }
                    
                    if (scoredPoints == null) {
                        log.warn("No points found in response");
                        return new ArrayList<>();
                    }
                    
                    List<Map<String, Object>> results = new ArrayList<>();
                    for (int i = 0; i < scoredPoints.size(); i++) {
                        JsonObject point = scoredPoints.get(i).getAsJsonObject();
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", point.get("id").getAsString());
                        map.put("score", point.get("score").getAsDouble());
                        
                        // 安全地解析 payload
                        JsonElement payloadElement = point.get("payload");
                        if (payloadElement != null && !payloadElement.isJsonNull()) {
                            map.put("payload", gson.fromJson(payloadElement, Map.class));
                        } else {
                            map.put("payload", new HashMap<>());
                        }
                        
                        results.add(map);
                    }
                    log.info("Search found {} similar points", results.size());
                    return results;
                } else {
                    throw new IOException("Search failed with code " + response.code() + ": " + responseBody);
                }
            }
        } catch (Exception e) {
            log.error("Failed to search similar vectors: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to search: " + e.getMessage(), e);
        }
    }
    
    public void deletePointsByDocumentId(String documentId) {
        try {
            JsonObject requestBody = new JsonObject();
            JsonObject filter = new JsonObject();
            JsonArray must = new JsonArray();
            
            JsonObject condition = new JsonObject();
            JsonObject fieldCondition = new JsonObject();
            JsonObject match = new JsonObject();
            
            match.addProperty("value", documentId);
            fieldCondition.addProperty("key", "document_id");
            fieldCondition.add("match", match);
            condition.add("field", fieldCondition);
            must.add(condition);
            
            filter.add("must", must);
            JsonObject pointsFilter = new JsonObject();
            pointsFilter.add("filter", filter);
            requestBody.add("points", pointsFilter);
            
            String json = gson.toJson(requestBody);
            RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));
            
            Request request = new Request.Builder()
                .url(baseUrl + "/collections/" + qdrantConfig.getCollectionName() + "/points/delete")
                .post(body)
                .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    log.info("Deleted points for document {}", documentId);
                } else {
                    log.warn("Delete response: {}", response.code());
                }
            }
        } catch (Exception e) {
            log.error("Failed to delete points: {}", e.getMessage());
        }
    }
    
    public boolean isInitialized() {
        return initialized;
    }
}
