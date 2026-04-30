package com.knowledge.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "entity_relations")
public class EntityRelation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "source_entity_id", nullable = false)
    private KnowledgeEntity sourceEntity;
    
    @ManyToOne
    @JoinColumn(name = "target_entity_id", nullable = false)
    private KnowledgeEntity targetEntity;
    
    @Column(nullable = false)
    private String relationType; // CO_OCCURRENCE, SUBJECT_OBJECT, KEYWORD
    
    @Column
    private Double weight;
    
    @Column
    private Long documentId;
}
