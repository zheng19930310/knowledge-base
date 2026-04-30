package com.knowledge.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.knowledge.config.AppConfig;
import com.knowledge.model.ChatHistory;
import com.knowledge.repository.ChatHistoryRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {
    
    private final QdrantService qdrantService;
    private final QianwenService qianwenService;
    private final ChatHistoryRepository chatHistoryRepository;
    private final AppConfig appConfig;
    
    @Data
    public static class ChatResponse {
        private String answer;
        private Double matchScore;
        private Double recallRate;
        private String sources;
        private List<Map<String, Object>> context;
        private Double confidence;  // 大模型返回的置信度
        private List<String> quotes;  // 引用的原文片段
    }
    
    public ChatResponse chat(String question, String sessionId, boolean memoryEnabled) {
        try {
            // Get embedding for question
            float[] questionVector = qianwenService.getEmbedding(question);
            
            // Search similar vectors in Qdrant
            List<Map<String, Object>> results = qdrantService.searchSimilar(
                questionVector, 
                appConfig.getTopK()
            );
            
            // 调试：输出搜索结果分数
            log.info("Search results for question '{}':", question);
            for (int i = 0; i < results.size(); i++) {
                double score = (double) results.get(i).get("score");
                Map<String, Object> payload = (Map<String, Object>) results.get(i).get("payload");
                String chunk = payload.getOrDefault("chunk", "").toString();
                String preview = chunk.length() > 50 ? chunk.substring(0, 50) + "..." : chunk;
                log.info("  Result {}: score={}, preview={}", i + 1, score, preview);
            }
            
            // Build context from search results
            List<Map<String, Object>> context = new ArrayList<>();
            StringBuilder contextText = new StringBuilder();
            Set<String> sourceFiles = new HashSet<>();
            double maxScore = 0.0;
            
            for (Map<String, Object> result : results) {
                double score = (double) result.get("score");
                double threshold = appConfig.getSimilarityThreshold();
                
                log.info("Checking result: score={}, threshold={}, pass={}", 
                    score, threshold, score >= threshold);
                
                if (score >= threshold) {
                    Map<String, Object> payload = (Map<String, Object>) result.get("payload");
                    
                    String chunk = payload.getOrDefault("chunk", "").toString();
                    String fileName = payload.getOrDefault("file_name", "").toString();
                    String filePath = payload.getOrDefault("file_path", "").toString();
                    
                    contextText.append("【文档片段】\n")
                        .append(chunk).append("\n\n");
                    
                    sourceFiles.add(fileName + " (" + filePath + ")");
                    
                    Map<String, Object> ctxItem = new HashMap<>();
                    ctxItem.put("chunk", chunk);
                    ctxItem.put("fileName", fileName);
                    ctxItem.put("filePath", filePath);
                    ctxItem.put("score", score);
                    context.add(ctxItem);
                    
                    if (score > maxScore) {
                        maxScore = score;
                    }
                }
            }
            
            // Calculate metrics
            double matchScore = maxScore * 100;
            double recallRate = results.size() > 0 ? 
                (double) context.size() / results.size() * 100 : 0.0;
            
            // Build prompt with or without context
            String prompt;
            String answer;
            
            if (contextText.length() == 0) {
                // No knowledge base content found - 完全未命中，交给大模型扩展
                log.info("No relevant knowledge found for question: {}", question);
                prompt = buildPromptWithoutContext(question);
                answer = qianwenService.chat(prompt, null);
                
                // Parse JSON response
                Map<String, Object> parsedResponse = parseJsonResponse(answer);
                String formattedAnswer = (String) parsedResponse.getOrDefault("answer", answer);
                Double confidence = (Double) parsedResponse.getOrDefault("confidence", 0.0);
                
                // Save to history
                if (sessionId != null) {
                    ChatHistory chatHistory = new ChatHistory();
                    chatHistory.setQuestion(question);
                    chatHistory.setAnswer(formattedAnswer);
                    chatHistory.setMatchScore(0.0);
                    chatHistory.setRecallRate(0.0);
                    chatHistory.setSources("");
                    chatHistory.setSessionId(sessionId);
                    chatHistoryRepository.save(chatHistory);
                }
                
                // Build response
                ChatResponse response = new ChatResponse();
                response.setAnswer(formattedAnswer);
                response.setMatchScore(0.0);
                response.setRecallRate(0.0);
                response.setSources("");
                response.setContext(new ArrayList<>());
                response.setConfidence(confidence);
                response.setQuotes((List<String>) parsedResponse.getOrDefault("quotes", new ArrayList<>()));
                
                return response;
            } else {
                // Has knowledge base content - 根据匹配度采用不同策略
                prompt = buildPromptWithContext(contextText.toString(), question, matchScore);
                
                // Get history if memory enabled
                List<Map<String, String>> history = null;
                if (memoryEnabled && sessionId != null) {
                    history = getChatHistory(sessionId);
                }
                
                // Call Qianwen API
                answer = qianwenService.chat(prompt, history);
                
                // Parse JSON response
                Map<String, Object> parsedResponse = parseJsonResponse(answer);
                String formattedAnswer = (String) parsedResponse.getOrDefault("answer", answer);
                Double confidence = (Double) parsedResponse.getOrDefault("confidence", matchScore / 100.0);
                @SuppressWarnings("unchecked")
                List<String> sourcesList = (List<String>) parsedResponse.getOrDefault("sources", new ArrayList<>());
                @SuppressWarnings("unchecked")
                List<String> quotesList = (List<String>) parsedResponse.getOrDefault("quotes", new ArrayList<>());
                
                // Merge sources from RAG and LLM
                String finalSources = sourcesList.isEmpty() ? 
                    String.join("; ", sourceFiles) : 
                    String.join("; ", sourcesList);
                
                // Save to history
                if (sessionId != null) {
                    ChatHistory chatHistory = new ChatHistory();
                    chatHistory.setQuestion(question);
                    chatHistory.setAnswer(formattedAnswer);
                    chatHistory.setMatchScore(matchScore);
                    chatHistory.setRecallRate(recallRate);
                    chatHistory.setSources(finalSources);
                    chatHistory.setSessionId(sessionId);
                    chatHistoryRepository.save(chatHistory);
                }
                
                // Build response
                ChatResponse response = new ChatResponse();
                response.setAnswer(formattedAnswer);
                response.setMatchScore(matchScore);
                response.setRecallRate(recallRate);
                response.setSources(finalSources);
                response.setContext(context);
                response.setConfidence(confidence);
                response.setQuotes(quotesList);
                
                return response;
            }
        } catch (Exception e) {
            log.error("Chat error: {}", e.getMessage(), e);
            throw new RuntimeException("Chat failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * 根据匹配度构建不同的Prompt策略
     * @param context 知识库上下文
     * @param question 用户问题
     * @param matchScore 匹配度（百分比）
     * @return 构建好的Prompt
     */
    private String buildPromptWithContext(String context, String question, double matchScore) {
        if (matchScore >= 30) {
            // 高匹配度：命中结果，适度扩展但贴近原文
            return "你是一个智能知识库助手，请基于以下知识库内容回答问题。\n\n" +
                   "【知识库内容】\n" + context + "\n\n" +
                   "【用户问题】\n" + question + "\n\n" +
                   "回答要求：\n" +
                   "1. 必须基于知识库内容回答，确保准确性\n" +
                   "2. 可以对内容进行适度扩展和补充说明，但不能偏离原文核心信息\n" +
                   "3. 扩展内容需要与知识库中的信息有明确的逻辑关联\n" +
                   "4. 如果知识库内容不完整，可以基于已有信息进行合理推断，但要说明这是推断\n" +
                   "\n" +
                   "请严格按照以下JSON格式回答（不要包含markdown代码块标记）：\n" +
                   "{\n" +
                   "  \"answer\": \"你的回答内容\",\n" +
                   "  \"sources\": [\"来源文件1\", \"来源文件2\"],\n" +
                   "  \"confidence\": 0.85,\n" +
                   "  \"quotes\": [\"引用的关键原文片段1\", \"引用的关键原文片段2\"]\n" +
                   "}\n\n" +
                   "说明：\n" +
                   "- answer: 基于知识库的详细回答，可以适度扩展但必须贴近原文\n" +
                   "- sources: 引用的来源文件列表\n" +
                   "- confidence: 回答置信度(0-1之间的小数)，基于知识库匹配度\n" +
                   "- quotes: 从知识库中引用的关键原文片段（最多3条）";
        } else {
            // 低匹配度：未完全命中，交给大模型梳理和扩展
            return "你是一个智能知识库助手，请处理以下情况。\n\n" +
                   "【知识库内容】（可能不完全相关）\n" + context + "\n\n" +
                   "【用户问题】\n" + question + "\n\n" +
                   "当前情况：知识库中的内容与您提出的问题相关性较低（匹配度约" + String.format("%.1f", matchScore) + "%）。\n\n" +
                   "回答要求：\n" +
                   "1. 先分析知识库中是否有部分相关信息可以利用\n" +
                   "2. 基于已有信息进行推理、归纳和扩展\n" +
                   "3. 如果知识库内容不够，可以用你的通用知识补充，但要明确区分哪些来自知识库、哪些是你的推理\n" +
                   "4. 给出尽可能有帮助的回答，即使知识库中没有直接答案\n" +
                   "5. 在回答末尾说明哪些是知识库内容、哪些是推理补充\n" +
                   "\n" +
                   "请严格按照以下JSON格式回答（不要包含markdown代码块标记）：\n" +
                   "{\n" +
                   "  \"answer\": \"你的回答内容\",\n" +
                   "  \"sources\": [\"来源文件1\", \"来源文件2\"],\n" +
                   "  \"confidence\": 0.5,\n" +
                   "  \"quotes\": [\"引用的关键原文片段1\"]\n" +
                   "}\n\n" +
                   "说明：\n" +
                   "- answer: 综合知识库和推理的详细回答，要说明信息来源\n" +
                   "- sources: 引用的来源文件列表\n" +
                   "- confidence: 回答置信度(0-1之间的小数)，低匹配度时通常0.3-0.6\n" +
                   "- quotes: 从知识库中引用的关键原文片段（如有）";
        }
    }
    
    private String buildPromptWithoutContext(String question) {
        return "你是一个智能知识库助手。用户询问：" + question + "\n\n" +
               "注意：当前知识库中没有相关内容。请严格按照以下JSON格式回答（不要包含markdown代码块标记）：\n" +
               "{\n" +
               "  \"answer\": \"告知用户知识库中暂无相关内容，并建议用户上传文档\",\n" +
               "  \"sources\": [],\n" +
               "  \"confidence\": 0.0,\n" +
               "  \"quotes\": []\n" +
               "}\n\n" +
               "请用友好的语气回答，并提供有用的建议。";
    }
    
    private String formatAnswer(String answer, Set<String> sources, double matchScore) {
        // 现在大模型返回JSON格式，直接返回即可
        // 如果需要向后兼容，可以保留旧的逻辑
        return answer;
    }
    
    private String formatAnswerWithoutKnowledge(String answer) {
        // 现在大模型返回JSON格式，直接返回即可
        return answer;
    }
    
    /**
     * 解析大模型返回的JSON响应
     * 支持带markdown代码块和不带代码块的格式
     */
    private Map<String, Object> parseJsonResponse(String response) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 尝试提取JSON（可能包含markdown代码块标记）
            String jsonStr = response;
            
            // 如果包含markdown代码块，提取其中的JSON
            if (response.contains("```json")) {
                int start = response.indexOf("```json") + 7;
                int end = response.indexOf("```", start);
                if (end > start) {
                    jsonStr = response.substring(start, end).trim();
                }
            } else if (response.contains("```")) {
                int start = response.indexOf("```") + 3;
                int end = response.indexOf("```", start);
                if (end > start) {
                    jsonStr = response.substring(start, end).trim();
                }
            }
            
            // 解析JSON
            JsonObject jsonObject = JsonParser.parseString(jsonStr).getAsJsonObject();
            
            result.put("answer", jsonObject.has("answer") ? jsonObject.get("answer").getAsString() : response);
            result.put("confidence", jsonObject.has("confidence") ? jsonObject.get("confidence").getAsDouble() : 0.5);
            
            // 解析sources数组
            List<String> sources = new ArrayList<>();
            if (jsonObject.has("sources") && jsonObject.get("sources").isJsonArray()) {
                com.google.gson.JsonArray sourcesArray = jsonObject.getAsJsonArray("sources");
                for (int i = 0; i < sourcesArray.size(); i++) {
                    sources.add(sourcesArray.get(i).getAsString());
                }
            }
            result.put("sources", sources);
            
            // 解析quotes数组
            List<String> quotes = new ArrayList<>();
            if (jsonObject.has("quotes") && jsonObject.get("quotes").isJsonArray()) {
                com.google.gson.JsonArray quotesArray = jsonObject.getAsJsonArray("quotes");
                for (int i = 0; i < quotesArray.size(); i++) {
                    quotes.add(quotesArray.get(i).getAsString());
                }
            }
            result.put("quotes", quotes);
            
            log.info("Successfully parsed JSON response");
            
        } catch (Exception e) {
            log.warn("Failed to parse JSON response, using raw text: {}", e.getMessage());
            // 如果解析失败，返回原始响应
            result.put("answer", response);
            result.put("confidence", 0.5);
            result.put("sources", new ArrayList<String>());
            result.put("quotes", new ArrayList<String>());
        }
        
        return result;
    }
    
    private List<Map<String, String>> getChatHistory(String sessionId) {
        List<ChatHistory> histories = chatHistoryRepository
            .findBySessionIdOrderByCreateTimeAsc(sessionId);
        
        int start = Math.max(0, histories.size() - appConfig.getMaxMemoryTurns());
        List<ChatHistory> recentHistories = histories.subList(start, histories.size());
        
        return recentHistories.stream().map(h -> {
            Map<String, String> msg = new HashMap<>();
            msg.put("question", h.getQuestion());
            msg.put("answer", h.getAnswer());
            return msg;
        }).collect(Collectors.toList());
    }
    
    public void clearHistory(String sessionId) {
        chatHistoryRepository.deleteBySessionId(sessionId);
    }
    
    public List<Map<String, Object>> getChatHistoryList(String sessionId) {
        List<ChatHistory> histories = chatHistoryRepository
            .findBySessionIdOrderByCreateTimeAsc(sessionId);
        
        return histories.stream().map(h -> {
            Map<String, Object> msg = new HashMap<>();
            msg.put("question", h.getQuestion());
            msg.put("answer", h.getAnswer());
            msg.put("matchScore", h.getMatchScore());
            msg.put("recallRate", h.getRecallRate());
            msg.put("sources", h.getSources());
            msg.put("createTime", h.getCreateTime());
            return msg;
        }).collect(Collectors.toList());
    }
}
