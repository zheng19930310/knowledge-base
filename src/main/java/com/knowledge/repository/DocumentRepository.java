package com.knowledge.repository;

import com.knowledge.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByOrderByUploadTimeDesc();
    Optional<Document> findByFileName(String fileName);
}
