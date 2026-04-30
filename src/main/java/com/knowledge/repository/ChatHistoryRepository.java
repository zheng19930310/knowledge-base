package com.knowledge.repository;

import com.knowledge.model.ChatHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatHistoryRepository extends JpaRepository<ChatHistory, Long> {
    List<ChatHistory> findBySessionIdOrderByCreateTimeAsc(String sessionId);
    void deleteBySessionId(String sessionId);
}
