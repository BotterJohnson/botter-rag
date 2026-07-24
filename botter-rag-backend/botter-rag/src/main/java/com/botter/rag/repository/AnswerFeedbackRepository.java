package com.botter.rag.repository;

import com.botter.rag.entity.AnswerFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AnswerFeedbackRepository extends JpaRepository<AnswerFeedback, Long> {
    Optional<AnswerFeedback> findByMessageIdAndUserId(Long messageId, Long userId);
}