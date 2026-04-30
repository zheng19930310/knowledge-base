package com.knowledge.repository;

import com.knowledge.model.EntityRelation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EntityRelationRepository extends JpaRepository<EntityRelation, Long> {
    List<EntityRelation> findBySourceEntityId(Long sourceEntityId);
    List<EntityRelation> findByTargetEntityId(Long targetEntityId);
    List<EntityRelation> findByDocumentId(Long documentId);
}
