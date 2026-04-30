package com.knowledge.controller;

import com.knowledge.service.KnowledgeGraphService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/graph")
@RequiredArgsConstructor
@CrossOrigin
public class KnowledgeGraphController {
    
    private final KnowledgeGraphService graphService;
    
    @PostMapping("/extract/{documentId}")
    public ResponseEntity<Map<String, Object>> extractEntities(@PathVariable Long documentId) {
        try {
            graphService.extractEntitiesFromDocument(documentId);
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
    
    @GetMapping
    public ResponseEntity<Map<String, Object>> getGraph() {
        try {
            Map<String, Object> graphData = graphService.getKnowledgeGraph();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", graphData);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @PostMapping("/rebuild")
    public ResponseEntity<Map<String, Object>> rebuildGraph() {
        try {
            graphService.rebuildKnowledgeGraph();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Knowledge graph rebuilt successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
