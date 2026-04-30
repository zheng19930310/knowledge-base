package com.knowledge.service;

import com.knowledge.model.Document;
import com.knowledge.repository.DocumentRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DocumentSyncTest {

    @Autowired
    private DocumentService documentService;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private QdrantService qdrantService;

    private static final Long TEST_DOCUMENT_ID = 33L; // 第一个上传的文件ID

    private static int testAttempts = 0;
    private static final int MAX_ATTEMPTS = 10;
    private static boolean testSuccess = false;

    @BeforeEach
    public void setUp() {
        System.out.println("\n========== Test Attempt " + (testAttempts + 1) + "/" + MAX_ATTEMPTS + " ==========");
        testAttempts++;
    }

    @Test
    @Order(1)
    @DisplayName("测试1: 检查文档是否存在")
    public void testDocumentExists() {
        System.out.println(">>> 检查文档 ID: " + TEST_DOCUMENT_ID);
        
        Document document = documentRepository.findById(TEST_DOCUMENT_ID).orElse(null);
        assertNotNull(document, "文档应该存在");
        
        System.out.println("✓ 文档存在: " + document.getOriginalFileName());
        System.out.println("  - 文件大小: " + document.getFileSize() + " bytes");
        System.out.println("  - 内容长度: " + (document.getContent() != null ? document.getContent().length() : 0) + " 字符");
    }

    @Test
    @Order(2)
    @DisplayName("测试2: 测试Qdrant连接")
    public void testQdrantConnection() {
        System.out.println(">>> 测试Qdrant连接");
        
        assertTrue(qdrantService.isInitialized(), "Qdrant应该已初始化");
        System.out.println("✓ Qdrant已初始化");
    }

    @Test
    @Order(3)
    @DisplayName("测试3: 测试Embedding API")
    public void testEmbeddingAPI() {
        System.out.println(">>> 测试Embedding API");
        
        Document document = documentRepository.findById(TEST_DOCUMENT_ID).orElseThrow();
        String testText = document.getContent().substring(0, Math.min(100, document.getContent().length()));
        
        System.out.println("  测试文本: " + testText.substring(0, Math.min(50, testText.length())) + "...");
        
        assertDoesNotThrow(() -> {
            float[] embedding = documentService.getQianwenService().getEmbedding(testText);
            assertNotNull(embedding, "Embedding不应为null");
            assertEquals(1536, embedding.length, "Embedding维度应该是1536");
            System.out.println("✓ Embedding API正常，维度: " + embedding.length);
        }, "Embedding API调用应该成功");
    }

    @Test
    @Order(4)
    @DisplayName("测试4: 完整同步流程")
    public void testFullSync() {
        System.out.println(">>> 开始完整同步测试");
        
        // 重置文档状态
        Document document = documentRepository.findById(TEST_DOCUMENT_ID).orElseThrow();
        document.setSyncedToVector(false);
        documentRepository.save(document);
        System.out.println("  已重置文档同步状态");
        
        // 执行同步
        assertDoesNotThrow(() -> {
            System.out.println("  开始同步...");
            documentService.syncToVector(TEST_DOCUMENT_ID);
            System.out.println("✓ 同步成功完成");
        }, "同步流程应该成功");
        
        // 验证同步状态
        document = documentRepository.findById(TEST_DOCUMENT_ID).orElseThrow();
        assertTrue(document.getSyncedToVector(), "文档应该标记为已同步");
        assertNotNull(document.getLastSyncTime(), "应该有最后同步时间");
        
        System.out.println("✓ 文档状态已更新: syncedToVector=true");
        System.out.println("  最后同步时间: " + document.getLastSyncTime());
        
        testSuccess = true;
    }

    @Test
    @Order(5)
    @DisplayName("测试5: 验证Qdrant中的数据")
    public void testQdrantData() {
        System.out.println(">>> 验证Qdrant中的数据");
        
        Document document = documentRepository.findById(TEST_DOCUMENT_ID).orElseThrow();
        String testText = document.getContent().substring(0, Math.min(100, document.getContent().length()));
        
        float[] embedding = documentService.getQianwenService().getEmbedding(testText);
        
        List<java.util.Map<String, Object>> results = qdrantService.searchSimilar(embedding, 5);
        
        System.out.println("  找到 " + results.size() + " 条相似记录");
        
        assertFalse(results.isEmpty(), "应该找到至少一条记录");
        
        boolean foundOwnDocument = results.stream()
            .anyMatch(r -> {
                Object payload = r.get("payload");
                if (payload instanceof java.util.Map) {
                    Object docId = ((java.util.Map<?, ?>) payload).get("document_id");
                    return TEST_DOCUMENT_ID.toString().equals(docId);
                }
                return false;
            });
        
        assertTrue(foundOwnDocument, "应该能找到自己的文档");
        System.out.println("✓ 成功验证Qdrant中的数据");
    }

    @AfterAll
    public static void tearDown() {
        System.out.println("\n========== 测试总结 ==========");
        System.out.println("总尝试次数: " + testAttempts + "/" + MAX_ATTEMPTS);
        System.out.println("测试结果: " + (testSuccess ? "✓ 成功" : " 失败"));
        
        if (!testSuccess && testAttempts >= MAX_ATTEMPTS) {
            System.out.println("\n⚠ 超过最大尝试次数，需要人工排查");
            System.out.println("请创建排查任务清单");
        }
    }
}
