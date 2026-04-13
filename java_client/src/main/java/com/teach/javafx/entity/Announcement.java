package com.teach.javafx.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 公告信息
 */
@Entity
@Table(name = "announcement")
public class Announcement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 标题 */
    @Column(nullable = false, length = 200)
    private String title;

    /** 内容 */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    /** 发布人用户名 */
    @Column(name = "publisher", length = 50)
    private String publisher;

    /** 类型：通知/公告/紧急 */
    @Column(length = 20)
    private String type = "通知";

    /** 状态：Published / Draft / Archived */
    @Column(length = 20)
    private String status = "Published";

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // ===== Getters & Setters =====
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getPublisher() { return publisher; }
    public void setPublisher(String publisher) { this.publisher = publisher; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
