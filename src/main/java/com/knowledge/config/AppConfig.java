package com.knowledge.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app")
public class AppConfig {
    private String uploadDir = "./uploads";
    private int chunkSize = 600;
    private int chunkOverlap = 50;
    private int topK = 5;
    private double similarityThreshold = 0.5;
    private boolean memoryEnabled = true;
    private int maxMemoryTurns = 10;
}
