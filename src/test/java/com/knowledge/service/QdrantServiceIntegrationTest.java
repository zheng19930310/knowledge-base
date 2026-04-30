package com.knowledge.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Slf4j
class QdrantServiceIntegrationTest {

    private OkHttpClient httpClient;
    private Gson gson;
    private String qdrantUrl;

    @BeforeEach
    void setUp() {
        httpClient = new OkHttpClient();
        gson = new Gson();
        qdrantUrl = "http://localhost:6333";
    }

    @Test
    void testSearch_CurrentImplementation() throws IOException {
        log.info("=== 测试1: 当前实现 (vector作为数组 - 已修复) ===");
        
        float[] testVector = new float[1536];
        for (int i = 0; i < testVector.length; i++) {
            testVector[i] = (float) (Math.random() * 0.1);
        }

        // 使用修复后的实现：vector 直接是数组
        JsonObject requestBody = new JsonObject();
        JsonArray vectorArray = new JsonArray();
        for (float v : testVector) {
            vectorArray.add(v);
        }
        requestBody.add("vector", vectorArray);
        requestBody.addProperty("limit", 5);
        requestBody.addProperty("with_payload", true);
        requestBody.addProperty("with_vector", false);

        String json = gson.toJson(requestBody);
        log.info("请求体(前200字符): {}", json.substring(0, Math.min(200, json.length())) + "...");

        RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url(qdrantUrl + "/collections/knowledge_base/points/search")
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            log.info("响应: {} - {}", response.code(), responseBody);
            
            if (response.isSuccessful()) {
                log.info("✅ 测试1通过 - 修复后的实现成功");
                JsonObject result = gson.fromJson(responseBody, JsonObject.class);
                assertTrue(result.has("result"), "响应应该包含 result 字段");
            } else {
                log.info("❌ 测试1失败: {}", responseBody);
                fail("搜索失败: " + responseBody);
            }
        }
    }

    @Test
    void testSearch_VectorAsArray() throws IOException {
        log.info("\n=== 测试2: vector直接作为数组 ===");
        
        float[] testVector = new float[1536];
        for (int i = 0; i < testVector.length; i++) {
            testVector[i] = (float) (Math.random() * 0.1);
        }

        JsonObject requestBody = new JsonObject();
        JsonArray vectorArray = new JsonArray();
        for (float v : testVector) {
            vectorArray.add(v);
        }
        requestBody.add("vector", vectorArray);
        requestBody.addProperty("limit", 5);
        requestBody.addProperty("with_payload", true);
        requestBody.addProperty("with_vector", false);

        String json = gson.toJson(requestBody);
        log.info("请求体(前200字符): {}", json.substring(0, Math.min(200, json.length())) + "...");

        RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url(qdrantUrl + "/collections/knowledge_base/points/search")
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            log.info("响应: {} - {}", response.code(), responseBody);
            
            if (response.isSuccessful()) {
                log.info("✅ 测试2通过");
            } else {
                log.info("❌ 测试2失败: {}", responseBody);
            }
        }
    }

    @Test
    void testSearch_MinimalRequest() throws IOException {
        log.info("\n=== 测试3: 最小化请求 ===");
        
        float[] testVector = new float[1536];
        for (int i = 0; i < testVector.length; i++) {
            testVector[i] = 0.01f;
        }

        JsonObject requestBody = new JsonObject();
        JsonArray vectorArray = new JsonArray();
        for (float v : testVector) {
            vectorArray.add(v);
        }
        
        requestBody.add("vector", vectorArray);
        requestBody.addProperty("limit", 1);

        String json = gson.toJson(requestBody);
        log.info("请求体(前200字符): {}", json.substring(0, Math.min(200, json.length())) + "...");

        RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url(qdrantUrl + "/collections/knowledge_base/points/search")
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            log.info("响应: {} - {}", response.code(), responseBody);
            
            if (response.isSuccessful()) {
                log.info("✅ 测试3通过");
            } else {
                log.info("❌ 测试3失败: {}", responseBody);
            }
        }
    }

    @Test
    void testGetCollectionInfo() throws IOException {
        log.info("\n=== 测试4: 检查集合信息 ===");
        
        Request request = new Request.Builder()
                .url(qdrantUrl + "/collections/knowledge_base")
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            log.info("集合信息: {} - {}", response.code(), responseBody);
            
            if (response.isSuccessful()) {
                JsonObject result = gson.fromJson(responseBody, JsonObject.class);
                if (result.has("result")) {
                    JsonObject collectionInfo = result.getAsJsonObject("result");
                    log.info("集合配置: {}", collectionInfo.toString());
                    
                    if (collectionInfo.has("config") && collectionInfo.getAsJsonObject("config").has("params")) {
                        JsonObject params = collectionInfo.getAsJsonObject("config").getAsJsonObject("params");
                        log.info("向量参数: {}", params.toString());
                    }
                }
            }
        }
    }
}
