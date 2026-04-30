package com.knowledge.controller;

import com.knowledge.model.Document;
import com.knowledge.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@CrossOrigin
public class DocumentController {
    
    private final DocumentService documentService;
    
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            Document document = documentService.uploadFile(file);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", document);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllDocuments() {
        List<Document> documents = documentService.getAllDocuments();
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", documents);
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteDocument(@PathVariable Long id) {
        try {
            documentService.deleteDocument(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @PostMapping("/{id}/sync")
    public ResponseEntity<Map<String, Object>> syncToVector(@PathVariable Long id) {
        try {
            // 异步执行同步操作
            documentService.syncToVectorAsync(id);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "同步任务已启动，正在后台处理中...");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @GetMapping("/{id}/sync-progress")
    public ResponseEntity<Map<String, Object>> getSyncProgress(@PathVariable Long id) {
        try {
            DocumentService.SyncProgress progress = documentService.getSyncProgress(id);
            Map<String, Object> response = new HashMap<>();
            
            if (progress != null) {
                response.put("success", true);
                response.put("data", progress);
            } else {
                response.put("success", false);
                response.put("message", "未找到同步进度信息");
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @PostMapping("/{id}/remove-from-vector")
    public ResponseEntity<Map<String, Object>> removeFromVector(@PathVariable Long id) {
        try {
            documentService.removeFromVector(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
