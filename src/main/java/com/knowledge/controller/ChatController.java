package com.knowledge.controller;

import com.knowledge.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@CrossOrigin
public class ChatController {
    
    private final ChatService chatService;
    
    @PostMapping
    public ResponseEntity<Map<String, Object>> chat(
            @RequestBody Map<String, Object> request) {
        try {
            String question = (String) request.get("question");
            String sessionId = (String) request.getOrDefault("sessionId", UUID.randomUUID().toString());
            Boolean memoryEnabled = (Boolean) request.getOrDefault("memoryEnabled", true);
            
            if (question == null || question.trim().isEmpty()) {
                throw new IllegalArgumentException("Question cannot be empty");
            }
            
            ChatService.ChatResponse response = chatService.chat(question, sessionId, memoryEnabled);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", response);
            result.put("sessionId", sessionId);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }
    
    @PostMapping("/clear")
    public ResponseEntity<Map<String, Object>> clearHistory(
            @RequestBody Map<String, String> request) {
        try {
            String sessionId = request.get("sessionId");
            if (sessionId != null) {
                chatService.clearHistory(sessionId);
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }
    
    @GetMapping("/history")
    public ResponseEntity<Map<String, Object>> getChatHistory(
            @RequestParam String sessionId) {
        try {
            List<Map<String, Object>> history = chatService.getChatHistoryList(sessionId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", history);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }
}
