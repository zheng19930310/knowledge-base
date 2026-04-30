package com.knowledge.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "qdrant")
public class QdrantConfig {
    private String host = "localhost";
    private int port = 6333;
    private String collectionName = "knowledge_base";
    private int vectorSize = 1536;
    private String distance = "Cosine";
}
