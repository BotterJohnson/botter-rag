package com.botter.rag.repository;


import com.botter.rag.entity.IndexTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IndexTaskRepository extends JpaRepository<IndexTask, Long> {

    Optional<IndexTask> findTopByDocIdOrderByCreatedAtDesc(Long docId);
}