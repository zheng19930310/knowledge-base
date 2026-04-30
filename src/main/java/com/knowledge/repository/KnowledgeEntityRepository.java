package com.knowledge.repository;

import com.knowledge.model.KnowledgeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KnowledgeEntityRepository extends JpaRepository<KnowledgeEntity, Long> {
    List<KnowledgeEntity> findByType(String type);
    Optional<KnowledgeEntity> findByNameAndType(String name, String type);
    List<KnowledgeEntity> findByDocumentId(Long documentId);
}
