package com.knowledge.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "documents")
public class Document {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String fileName;
    
    @Column(nullable = false)
    private String originalFileName;
    
    @Column(nullable = false)
    private String fileType;
    
    @Column(nullable = false)
    private Long fileSize;
    
    @Column(columnDefinition = "TEXT")
    private String content;
    
    @Column(nullable = false)
    private String filePath;
    
    @Column
    private Boolean syncedToVector = false;
    
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime uploadTime;
    
    @Column
    private LocalDateTime lastSyncTime;
}
