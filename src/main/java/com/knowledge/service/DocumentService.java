package com.knowledge.service;

import com.knowledge.config.AppConfig;
import com.knowledge.model.Document;
import com.knowledge.repository.DocumentRepository;
import com.knowledge.util.DocumentParser;
import com.knowledge.util.TextChunker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {
    
    private final DocumentRepository documentRepository;
    private final QdrantService qdrantService;
    private final QianwenService qianwenService;
    private final TextChunker textChunker;
    private final AppConfig appConfig;
    private final KnowledgeGraphService knowledgeGraphService;
    
    // Getter methods for testing
    public QianwenService getQianwenService() {
        return qianwenService;
    }
    
    public QdrantService getQdrantService() {
        return qdrantService;
    }
    
    // 同步进度跟踪
    private final Map<Long, SyncProgress> syncProgressMap = new ConcurrentHashMap<>();
    
    @lombok.Data
    public static class SyncProgress {
        private Long documentId;
        private String fileName;
        private int totalChunks;
        private int processedChunks;
        private int progress; // 0-100
        private String status; // PROCESSING, COMPLETED, FAILED
        private String message;
        private LocalDateTime startTime;
        private LocalDateTime updateTime;
    }
    
    public Document uploadFile(MultipartFile file) throws IOException {
        String originalFileName = file.getOriginalFilename();
        String fileType = getFileType(originalFileName);
        
        // Validate file type
        if (!isValidFileType(fileType)) {
            throw new IllegalArgumentException("Unsupported file type: " + fileType);
        }
        
        // Create upload directory
        Path uploadPath = Paths.get(appConfig.getUploadDir());
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        // Generate unique filename
        String fileName = UUID.randomUUID().toString() + "_" + originalFileName;
        Path filePath = uploadPath.resolve(fileName);
        
        // Save file
        Files.copy(file.getInputStream(), filePath);
        
        // Parse content
        String content = DocumentParser.parseFile(filePath.toFile(), fileType);
        
        // Create document entity
        Document document = new Document();
        document.setFileName(fileName);
        document.setOriginalFileName(originalFileName);
        document.setFileType(fileType);
        document.setFileSize(file.getSize());
        document.setContent(content);
        document.setFilePath(filePath.toString());
        document.setSyncedToVector(false);
        
        return documentRepository.save(document);
    }
    
    public List<Document> getAllDocuments() {
        return documentRepository.findByOrderByUploadTimeDesc();
    }
    
    public void deleteDocument(Long id) {
        Document document = documentRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Document not found"));
        
        // Delete from vector database
        if (document.getSyncedToVector()) {
            qdrantService.deletePointsByDocumentId(String.valueOf(id));
        }
        
        // Delete file
        try {
            Files.deleteIfExists(Paths.get(document.getFilePath()));
        } catch (IOException e) {
            log.error("Failed to delete file: {}", e.getMessage());
        }
        
        // Delete from database
        documentRepository.delete(document);
    }
    
    public void syncToVector(Long id) {
        Document document = documentRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Document not found"));
        
        if (document.getContent() == null || document.getContent().isEmpty()) {
            throw new RuntimeException("Document content is empty");
        }
        
        log.info("Starting sync for document: {}", document.getOriginalFileName());
        
        // 记录文档内容大小
        int contentLength = document.getContent() != null ? document.getContent().length() : 0;
        log.info("Document content length: {} characters ({} KB)", contentLength, contentLength / 1024);
        
        // 初始化进度跟踪
        SyncProgress progress = new SyncProgress();
        progress.setDocumentId(id);
        progress.setFileName(document.getOriginalFileName());
        progress.setStatus("PROCESSING");
        progress.setMessage("开始同步...");
        progress.setStartTime(LocalDateTime.now());
        progress.setUpdateTime(LocalDateTime.now());
        syncProgressMap.put(id, progress);
        
        try {
            // Chunk text
            List<String> chunks = textChunker.chunkText(document.getContent());
            progress.setTotalChunks(chunks.size());
            log.info("Document chunked into {} parts", chunks.size());
            log.info("Average chunk size: {} characters", contentLength / Math.max(chunks.size(), 1));
            
            // Get embeddings and prepare payloads
            List<float[]> vectors = new ArrayList<>();
            List<Map<String, Object>> payloads = new ArrayList<>();
            
            int processed = 0;
            int batchSize = 1; // 每次只处理1个chunk，极大降低CPU使用率
            
            for (int i = 0; i < chunks.size(); i++) {
                String chunk = chunks.get(i);
                
                // 获取embedding
                float[] embedding = qianwenService.getEmbedding(chunk);
                vectors.add(embedding);
                
                Map<String, Object> payload = new HashMap<>();
                payload.put("document_id", String.valueOf(id));
                payload.put("file_name", document.getOriginalFileName());
                payload.put("file_path", document.getFilePath());
                payload.put("chunk", chunk);
                payloads.add(payload);
                
                processed++;
                
                // 更新进度
                progress.setProcessedChunks(processed);
                progress.setProgress((int) ((double) processed / chunks.size() * 100));
                progress.setMessage(String.format("正在处理 %d/%d 个文本块", processed, chunks.size()));
                progress.setUpdateTime(LocalDateTime.now());
                
                // 每处理一个chunk后都暂停，极大降低CPU使用率
                if (processed < chunks.size()) {
                    try {
                        Thread.sleep(500); // 每个chunk之间暂停500ms，大幅降低CPU使用率
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
            
            log.info("Upserting {} vectors to Qdrant...", vectors.size());
            progress.setMessage("正在上传到矢量库...");
            progress.setUpdateTime(LocalDateTime.now());
            
            // Upsert to Qdrant
            qdrantService.upsertPoints(String.valueOf(id), vectors, payloads);
            
            // 更新文档状态
            document.setSyncedToVector(true);
            document.setLastSyncTime(LocalDateTime.now());
            documentRepository.save(document);
            
            // 更新进度为完成
            progress.setProgress(100);
            progress.setStatus("COMPLETED");
            progress.setMessage("同步完成");
            progress.setUpdateTime(LocalDateTime.now());
            
            log.info("Sync completed for document: {}", document.getOriginalFileName());
            
            // 需求3：同步完成后自动更新知识图谱
            try {
                log.info("Starting knowledge graph extraction for document: {}", id);
                progress.setMessage("正在更新知识图谱...");
                progress.setUpdateTime(LocalDateTime.now());
                
                knowledgeGraphService.extractEntitiesFromDocument(id);
                
                log.info("Knowledge graph extraction completed for document: {}", id);
                progress.setMessage("同步和知识图谱更新完成");
            } catch (Exception e) {
                log.error("Failed to extract knowledge graph for document: {}", id, e);
                progress.setMessage("同步完成，但知识图谱更新失败: " + e.getMessage());
            }
            
        } catch (Exception e) {
            log.error("Sync failed for document: {}", document.getOriginalFileName(), e);
            progress.setStatus("FAILED");
            progress.setMessage("同步失败: " + e.getMessage());
            progress.setUpdateTime(LocalDateTime.now());
            
            // 更新文档状态为失败
            try {
                document.setSyncedToVector(false);
                documentRepository.save(document);
            } catch (Exception ex) {
                log.error("Failed to update document status after sync failure", ex);
            }
            
            throw new RuntimeException("Sync failed: " + e.getMessage(), e);
        }
    }
    
    @org.springframework.scheduling.annotation.Async("taskExecutor")
    public void syncToVectorAsync(Long id) {
        try {
            log.info("Async sync started for document ID: {}", id);
            syncToVector(id);
            log.info("Async sync completed successfully for document ID: {}", id);
        } catch (Exception e) {
            log.error("Async sync failed for document ID: {}", id, e);
            // 更新文档状态为失败
            try {
                Document document = documentRepository.findById(id).orElse(null);
                if (document != null) {
                    document.setSyncedToVector(false);
                    documentRepository.save(document);
                }
            } catch (Exception ex) {
                log.error("Failed to update document status after sync failure", ex);
            }
        }
    }
    
    public void removeFromVector(Long id) {
        Document document = documentRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Document not found"));
        
        qdrantService.deletePointsByDocumentId(String.valueOf(id));
        
        document.setSyncedToVector(false);
        document.setLastSyncTime(null);
        documentRepository.save(document);
        
        // 移除进度跟踪
        syncProgressMap.remove(id);
    }
    
    /**
     * 获取同步进度
     */
    public SyncProgress getSyncProgress(Long documentId) {
        return syncProgressMap.get(documentId);
    }
    
    /**
     * 获取所有同步进度
     */
    public Map<Long, SyncProgress> getAllSyncProgress() {
        return new HashMap<>(syncProgressMap);
    }
    
    private String getFileType(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }
    
    private boolean isValidFileType(String fileType) {
        return "pdf".equals(fileType) || "docx".equals(fileType) || 
               "doc".equals(fileType) || "txt".equals(fileType);
    }
}
