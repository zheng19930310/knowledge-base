package com.knowledge.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "qianwen")
public class QianwenConfig {
    private String apiKey;
    private String chatApiUrl = "https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation";
    private String embeddingApiUrl = "https://dashscope.aliyuncs.com/api/v1/services/embeddings/text-embedding/text-embedding";
    private String model = "qwen-turbo";
    private String embeddingModel = "text-embedding-v2";
    private int timeout = 30000;
}
