package com.knowledge.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "chat_history")
public class ChatHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String question;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String answer;
    
    @Column
    private Double matchScore;
    
    @Column
    private Double recallRate;
    
    @Column(columnDefinition = "TEXT")
    private String sources;
    
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createTime;
    
    @Column
    private String sessionId;
}
