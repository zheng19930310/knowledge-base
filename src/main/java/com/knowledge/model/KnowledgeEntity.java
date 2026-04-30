package com.knowledge.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "knowledge_entities")
public class KnowledgeEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(nullable = false)
    private String type; // PERSON, LOCATION, ORGANIZATION, TERM
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column
    private Integer frequency;
    
    @Column
    private Long documentId;
}
