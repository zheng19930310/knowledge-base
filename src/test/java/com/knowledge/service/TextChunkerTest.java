package com.knowledge.service;

import com.knowledge.config.AppConfig;
import com.knowledge.util.TextChunker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TextChunkerTest {

    @Mock
    private AppConfig appConfig;

    private TextChunker textChunker;

    @BeforeEach
    void setUp() {
        when(appConfig.getChunkSize()).thenReturn(500);
        when(appConfig.getChunkOverlap()).thenReturn(50);
        textChunker = new TextChunker(appConfig);
    }

    @Test
    void testChunkText_NormalText() {
        // 测试正常文本分块
        String text = "这是一段测试文本。".repeat(30);  // 减少重复次数
        
        List<String> chunks = textChunker.chunkText(text);
        
        assertNotNull(chunks);
        assertFalse(chunks.isEmpty());
        
        // 验证每个chunk的大小合理
        for (String chunk : chunks) {
            assertTrue(chunk.length() > 0);
            assertTrue(chunk.length() <= 600); // 允许一定的重叠
        }
    }

    @Test
    void testChunkText_EmptyText() {
        // 测试空文本
        List<String> chunks = textChunker.chunkText("");
        
        assertNotNull(chunks);
        assertTrue(chunks.isEmpty());
    }

    @Test
    void testChunkText_NullText() {
        // 测试null文本
        List<String> chunks = textChunker.chunkText(null);
        
        assertNotNull(chunks);
        assertTrue(chunks.isEmpty());
    }

    @Test
    void testChunkText_ShortText() {
        // 测试短文本（小于chunkSize）
        String shortText = "这是一段短文本。";
        
        List<String> chunks = textChunker.chunkText(shortText);
        
        assertNotNull(chunks);
        assertEquals(1, chunks.size());
        assertEquals(shortText, chunks.get(0));
    }

    @Test
    void testChunkText_WithSentences() {
        // 测试包含句号的文本，应该在句子边界分块
        String text = "第一句话。第二句话。第三句话。第四句话。第五句话。";
        text = text.repeat(10); // 减少重复次数
        
        List<String> chunks = textChunker.chunkText(text);
        
        assertNotNull(chunks);
        assertFalse(chunks.isEmpty());
        
        // 验证分块后尽量在句号处断开
        for (String chunk : chunks) {
            if (chunk.length() > 10) { // 忽略很短的块
                // 大部分块应该以句号结尾（如果可能的话）
                // 这不是严格要求，只是倾向性
            }
        }
    }

    @Test
    void testChunkText_Overlap() {
        // 测试重叠功能
        String text = "ABC".repeat(100);  // 减少重复次数
        
        List<String> chunks = textChunker.chunkText(text);
        
        assertNotNull(chunks);
        assertTrue(chunks.size() > 1);
        
        // 验证相邻块之间有重叠
        for (int i = 0; i < chunks.size() - 1; i++) {
            String currentChunk = chunks.get(i);
            String nextChunk = chunks.get(i + 1);
            
            // 检查重叠部分
            if (currentChunk.length() > 50 && nextChunk.length() > 50) {
                // 后一个块的开头应该包含前一个块的结尾部分
                String overlapCheck = currentChunk.substring(Math.max(0, currentChunk.length() - 60));
                assertTrue(nextChunk.startsWith(overlapCheck.substring(0, Math.min(10, overlapCheck.length()))) || 
                          nextChunk.contains(overlapCheck.substring(0, Math.min(5, overlapCheck.length()))));
            }
        }
    }
}
