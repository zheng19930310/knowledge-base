package com.knowledge.service;

import com.knowledge.model.Document;
import com.knowledge.model.KnowledgeEntity;
import com.knowledge.model.EntityRelation;
import com.knowledge.repository.DocumentRepository;
import com.knowledge.repository.KnowledgeEntityRepository;
import com.knowledge.repository.EntityRelationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeGraphService {
    
    private final DocumentRepository documentRepository;
    private final KnowledgeEntityRepository entityRepository;
    private final EntityRelationRepository relationRepository;
    
    public void extractEntitiesFromDocument(Long documentId) {
        log.info("Starting entity extraction for document {}", documentId);
        
        Document document = documentRepository.findById(documentId)
            .orElseThrow(() -> new RuntimeException("Document not found"));
        
        if (document.getContent() == null) {
            log.warn("Document content is null, skipping entity extraction");
            return;
        }
        
        String content = document.getContent();
        log.info("Document content length: {} characters", content.length());
        
        try {
            // Extract entities using simple patterns
            log.info("Extracting person names...");
            extractPersonNames(content, documentId);
            
            log.info("Extracting locations...");
            extractLocations(content, documentId);
            
            log.info("Extracting organizations...");
            extractOrganizations(content, documentId);
            
            log.info("Extracting terms...");
            extractTerms(content, documentId);
            
            // Create relations based on co-occurrence
            log.info("Creating co-occurrence relations...");
            createCoOccurrenceRelations(documentId);
            
            log.info("Entity extraction completed for document {}", documentId);
        } catch (Exception e) {
            log.error("Error during entity extraction for document {}", documentId, e);
            throw new RuntimeException("Entity extraction failed: " + e.getMessage(), e);
        }
    }
    
    public Map<String, Object> getKnowledgeGraph() {
        List<KnowledgeEntity> entities = entityRepository.findAll();
        List<EntityRelation> relations = relationRepository.findAll();
        
        // Convert to ECharts format
        List<Map<String, Object>> nodes = entities.stream().map(entity -> {
            Map<String, Object> node = new HashMap<>();
            node.put("id", entity.getId());
            node.put("name", entity.getName());
            node.put("category", entity.getType());
            node.put("value", entity.getFrequency() != null ? entity.getFrequency() : 1);
            return node;
        }).collect(Collectors.toList());
        
        List<Map<String, Object>> links = relations.stream().map(relation -> {
            Map<String, Object> link = new HashMap<>();
            link.put("source", relation.getSourceEntity().getId());
            link.put("target", relation.getTargetEntity().getId());
            link.put("relationType", relation.getRelationType());
            link.put("value", relation.getWeight() != null ? relation.getWeight() : 1.0);
            return link;
        }).collect(Collectors.toList());
        
        Map<String, Object> result = new HashMap<>();
        result.put("nodes", nodes);
        result.put("links", links);
        
        return result;
    }
    
    /**
     * 清理重复实体并重建知识图谱
     */
    public void rebuildKnowledgeGraph() {
        log.info("Starting knowledge graph rebuild...");
        
        // 1. 获取所有实体
        List<KnowledgeEntity> allEntities = entityRepository.findAll();
        
        // 2. 按名称和类型分组，找出重复的实体
        Map<String, List<KnowledgeEntity>> entityGroups = allEntities.stream()
            .collect(Collectors.groupingBy(e -> e.getName() + "_" + e.getType()));
        
        int duplicateCount = 0;
        int mergedCount = 0;
        
        // 3. 合并重复实体
        for (Map.Entry<String, List<KnowledgeEntity>> entry : entityGroups.entrySet()) {
            List<KnowledgeEntity> duplicates = entry.getValue();
            
            if (duplicates.size() > 1) {
                duplicateCount += duplicates.size() - 1;
                
                // 保留第一个实体，合并其他实体的频率
                KnowledgeEntity primary = duplicates.get(0);
                int totalFrequency = primary.getFrequency() != null ? primary.getFrequency() : 0;
                
                for (int i = 1; i < duplicates.size(); i++) {
                    KnowledgeEntity duplicate = duplicates.get(i);
                    if (duplicate.getFrequency() != null) {
                        totalFrequency += duplicate.getFrequency();
                    }
                    
                    // 更新指向重复实体的关系到主实体
                    updateRelationsToPrimary(duplicate.getId(), primary.getId());
                    
                    // 删除重复实体
                    entityRepository.delete(duplicate);
                    mergedCount++;
                }
                
                // 更新主实体的频率
                primary.setFrequency(totalFrequency);
                entityRepository.save(primary);
            }
        }
        
        log.info("Knowledge graph rebuild completed: {} duplicates found, {} entities merged", duplicateCount, mergedCount);
    }
    
    /**
     * 更新指向旧实体的关系到新实体
     */
    private void updateRelationsToPrimary(Long oldEntityId, Long newEntityId) {
        List<EntityRelation> relations = relationRepository.findAll();
        
        for (EntityRelation relation : relations) {
            boolean updated = false;
            
            if (relation.getSourceEntity().getId().equals(oldEntityId)) {
                relation.setSourceEntity(entityRepository.findById(newEntityId).orElseThrow());
                updated = true;
            }
            
            if (relation.getTargetEntity().getId().equals(oldEntityId)) {
                relation.setTargetEntity(entityRepository.findById(newEntityId).orElseThrow());
                updated = true;
            }
            
            if (updated) {
                relationRepository.save(relation);
            }
        }
    }
    
    private void extractPersonNames(String content, Long documentId) {
        // Simple pattern for Chinese names (2-4 characters)
        Pattern pattern = Pattern.compile("[\\u4e00-\\u9fa5]{2,4}");
        Matcher matcher = pattern.matcher(content);
        
        Map<String, Integer> nameFreq = new HashMap<>();
        while (matcher.find()) {
            String name = matcher.group();
            nameFreq.merge(name, 1, Integer::sum);
        }
        
        int savedCount = 0;
        int updatedCount = 0;
        int maxEntities = 50; // 限制最多保存50个实体，防止内存溢出
        
        for (Map.Entry<String, Integer> entry : nameFreq.entrySet()) {
            if (entry.getValue() >= 2 && savedCount < maxEntities) { // Only save names appearing 2+ times
                String name = entry.getKey();
                
                // 检查是否已存在同名同类型实体
                Optional<KnowledgeEntity> existingEntity = entityRepository.findByNameAndType(name, "PERSON");
                
                if (existingEntity.isPresent()) {
                    // 更新频率
                    KnowledgeEntity entity = existingEntity.get();
                    entity.setFrequency(entity.getFrequency() + entry.getValue());
                    entityRepository.save(entity);
                    updatedCount++;
                } else {
                    // 创建新实体
                    KnowledgeEntity entity = new KnowledgeEntity();
                    entity.setName(name);
                    entity.setType("PERSON");
                    entity.setFrequency(entry.getValue());
                    entity.setDocumentId(documentId);
                    entityRepository.save(entity);
                    savedCount++;
                }
            }
        }
        
        log.info("Saved {} new person entities, updated {} existing", savedCount, updatedCount);
    }
    
    private void extractLocations(String content, Long documentId) {
        // Simple pattern for locations (contains 省、市、县 etc.)
        Pattern pattern = Pattern.compile("[\\u4e00-\\u9fa5]+(?:省|市|县|区|镇)");
        Matcher matcher = pattern.matcher(content);
        
        Map<String, Integer> locationFreq = new HashMap<>();
        while (matcher.find()) {
            String location = matcher.group();
            locationFreq.merge(location, 1, Integer::sum);
        }
        
        int savedCount = 0;
        int updatedCount = 0;
        int maxEntities = 30;
        
        for (Map.Entry<String, Integer> entry : locationFreq.entrySet()) {
            if (savedCount + updatedCount >= maxEntities) break;
            
            String location = entry.getKey();
            
            // 检查是否已存在同名同类型实体
            Optional<KnowledgeEntity> existingEntity = entityRepository.findByNameAndType(location, "LOCATION");
            
            if (existingEntity.isPresent()) {
                // 更新频率
                KnowledgeEntity entity = existingEntity.get();
                entity.setFrequency(entity.getFrequency() + entry.getValue());
                entityRepository.save(entity);
                updatedCount++;
            } else {
                // 创建新实体
                KnowledgeEntity entity = new KnowledgeEntity();
                entity.setName(location);
                entity.setType("LOCATION");
                entity.setFrequency(entry.getValue());
                entity.setDocumentId(documentId);
                entityRepository.save(entity);
                savedCount++;
            }
        }
        
        log.info("Saved {} new location entities, updated {} existing", savedCount, updatedCount);
    }
    
    private void extractOrganizations(String content, Long documentId) {
        // Simple pattern for organizations (contains 公司、局、部 etc.)
        Pattern pattern = Pattern.compile("[\\u4e00-\\u9fa5]+(?:公司|局|部|委员会|协会|学校)");
        Matcher matcher = pattern.matcher(content);
        
        Map<String, Integer> orgFreq = new HashMap<>();
        while (matcher.find()) {
            String org = matcher.group();
            orgFreq.merge(org, 1, Integer::sum);
        }
        
        int savedCount = 0;
        int updatedCount = 0;
        int maxEntities = 30;
        
        for (Map.Entry<String, Integer> entry : orgFreq.entrySet()) {
            if (savedCount + updatedCount >= maxEntities) break;
            
            String org = entry.getKey();
            
            // 检查是否已存在同名同类型实体
            Optional<KnowledgeEntity> existingEntity = entityRepository.findByNameAndType(org, "ORGANIZATION");
            
            if (existingEntity.isPresent()) {
                // 更新频率
                KnowledgeEntity entity = existingEntity.get();
                entity.setFrequency(entity.getFrequency() + entry.getValue());
                entityRepository.save(entity);
                updatedCount++;
            } else {
                // 创建新实体
                KnowledgeEntity entity = new KnowledgeEntity();
                entity.setName(org);
                entity.setType("ORGANIZATION");
                entity.setFrequency(entry.getValue());
                entity.setDocumentId(documentId);
                entityRepository.save(entity);
                savedCount++;
            }
        }
        
        log.info("Saved {} new organization entities, updated {} existing", savedCount, updatedCount);
    }
    
    private void extractTerms(String content, Long documentId) {
        // Extract technical terms (capitalized words or specific patterns)
        Pattern pattern = Pattern.compile("[A-Z][a-zA-Z]+");
        Matcher matcher = pattern.matcher(content);
        
        Map<String, Integer> termFreq = new HashMap<>();
        while (matcher.find()) {
            String term = matcher.group();
            if (term.length() > 3) {
                termFreq.merge(term, 1, Integer::sum);
            }
        }
        
        int savedCount = 0;
        int updatedCount = 0;
        int maxEntities = 30;
        
        for (Map.Entry<String, Integer> entry : termFreq.entrySet()) {
            if (entry.getValue() >= 2 && savedCount + updatedCount < maxEntities) {
                String term = entry.getKey();
                
                // 检查是否已存在同名同类型实体
                Optional<KnowledgeEntity> existingEntity = entityRepository.findByNameAndType(term, "TERM");
                
                if (existingEntity.isPresent()) {
                    // 更新频率
                    KnowledgeEntity entity = existingEntity.get();
                    entity.setFrequency(entity.getFrequency() + entry.getValue());
                    entityRepository.save(entity);
                    updatedCount++;
                } else {
                    // 创建新实体
                    KnowledgeEntity entity = new KnowledgeEntity();
                    entity.setName(term);
                    entity.setType("TERM");
                    entity.setFrequency(entry.getValue());
                    entity.setDocumentId(documentId);
                    entityRepository.save(entity);
                    savedCount++;
                }
            }
        }
        
        log.info("Saved {} new term entities, updated {} existing", savedCount, updatedCount);
    }
    
    private void createCoOccurrenceRelations(Long documentId) {
        List<KnowledgeEntity> entities = entityRepository.findByDocumentId(documentId);
        
        log.info("Creating relations for {} entities", entities.size());
        
        // Create relations between entities that appear in same document
        int savedCount = 0;
        int maxRelations = 100; // 限制最多100个关系，防止内存溢出
        
        for (int i = 0; i < entities.size() && savedCount < maxRelations; i++) {
            for (int j = i + 1; j < entities.size() && savedCount < maxRelations; j++) {
                EntityRelation relation = new EntityRelation();
                relation.setSourceEntity(entities.get(i));
                relation.setTargetEntity(entities.get(j));
                relation.setRelationType("CO_OCCURRENCE");
                relation.setWeight(1.0);
                relation.setDocumentId(documentId);
                relationRepository.save(relation);
                savedCount++;
            }
        }
        
        log.info("Saved {} relations", savedCount);
    }
}
