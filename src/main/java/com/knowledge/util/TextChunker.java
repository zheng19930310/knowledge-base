package com.knowledge.util;

import com.knowledge.config.AppConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TextChunker {
    
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TextChunker.class);
    
    private final AppConfig appConfig;
    
    public List<String> chunkText(String text) {
        List<String> chunks = new ArrayList<>();
        int chunkSize = appConfig.getChunkSize();
        int overlap = appConfig.getChunkOverlap();
        
        if (text == null || text.isEmpty()) {
            return chunks;
        }
        
        // 清理文本：移除控制字符和多余空白
        StringBuilder cleaned = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            // 保留普通字符、中文、标点、换行
            if (Character.isLetterOrDigit(c) || Character.isWhitespace(c) || 
                c == '。' || c == '.' || c == ',' || c == '，' || c == '!' || c == '！' || 
                c == '?' || c == '？' || c == ':' || c == '：' || c == ';' || c == '；') {
                cleaned.append(c);
            } else if (c == '\n' || c == '\r') {
                cleaned.append(' '); // 将换行符替换为空格
            }
            // 忽略其他控制字符
        }
        text = cleaned.toString();
        
        int start = 0;
        int maxIterations = (text.length() / (chunkSize - overlap)) + 10; // 防止死循环
        int iterations = 0;
        
        while (start < text.length() && iterations < maxIterations) {
            iterations++;
            int end = Math.min(start + chunkSize, text.length());
            
            // Try to break at sentence or word boundary (限制搜索范围)
            if (end < text.length()) {
                // 只在当前chunk范围内查找，而不是从开头查找
                int searchStart = Math.max(start, end - 200); // 只搜索最后200个字符
                int lastSentence = text.lastIndexOf("。", end);
                int lastPeriod = text.lastIndexOf(".", end);
                int lastNewline = text.lastIndexOf("\n", end);
                
                int breakPoint = Math.max(lastSentence, Math.max(lastPeriod, lastNewline));
                
                // 确保breakPoint在合理范围内
                if (breakPoint > searchStart && breakPoint < end) {
                    end = breakPoint + 1;
                }
            }
            
            String chunk = text.substring(start, end).trim();
            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }
            
            start = end - overlap;
            if (start >= end) { // 防止死循环
                start = end;
            }
        }
        
        if (iterations >= maxIterations) {
            log.warn("TextChunker reached max iterations: {} for text length: {}", iterations, text.length());
        }
        
        return chunks;
    }
    
    /**
     * 手动去除首尾空白字符，避免创建额外的Pattern对象
     */
    private String removeLeadingTrailingWhitespace(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        
        int start = 0;
        int end = str.length();
        
        // 去除前导空白
        while (start < end && Character.isWhitespace(str.charAt(start))) {
            start++;
        }
        
        // 去除尾部空白
        while (end > start && Character.isWhitespace(str.charAt(end - 1))) {
            end--;
        }
        
        return str.substring(start, end);
    }
}
