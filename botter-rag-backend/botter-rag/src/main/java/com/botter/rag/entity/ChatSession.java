package com.botter.rag.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "kb_chat_session")
@Data
public class ChatSession {

    @Id
    private String id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String kbIds;

    private String title;

    @Column(nullable = false)
    private Integer messageCount = 0;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime lastActiveAt = LocalDateTime.now();

    @Column(nullable = false)
    private Boolean isDeleted = false;
}