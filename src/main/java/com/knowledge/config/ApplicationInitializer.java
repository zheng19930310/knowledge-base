package com.knowledge.config;

import com.knowledge.service.QdrantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApplicationInitializer implements CommandLineRunner {
    
    private final QdrantService qdrantService;
    
    @Override
    public void run(String... args) throws Exception {
        log.info("Initializing application...");
        
        // 输出JVM内存信息
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        
        log.info("=== JVM Memory Info ===");
        log.info("Max Memory: {} MB", maxMemory / (1024 * 1024));
        log.info("Total Memory: {} MB", totalMemory / (1024 * 1024));
        log.info("Free Memory: {} MB", freeMemory / (1024 * 1024));
        log.info("Used Memory: {} MB", (totalMemory - freeMemory) / (1024 * 1024));
        log.info("=======================");
        
        // Create Qdrant collection if not exists
        try {
            if (qdrantService.isInitialized()) {
                qdrantService.createCollection();
                log.info("Qdrant collection initialized");
            } else {
                log.warn("Qdrant client not initialized, skipping collection creation");
            }
        } catch (Exception e) {
            log.error("Failed to initialize Qdrant collection: {}", e.getMessage());
            log.warn("Application will continue without Qdrant features");
        }
        
        log.info("Application initialization completed");
    }
}
