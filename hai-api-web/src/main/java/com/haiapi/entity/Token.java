package com.haiapi.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;

@Data
@TableName("tokens")
@Entity
@EntityListeners(AuditingEntityListener.class)
public class Token {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "key_hash", nullable = false, unique = true, length = 64)
    private String keyHash;

    @TableField(fill = FieldFill.INSERT)
    @Column(name = "user_id")
    private String userId;

    @Column(columnDefinition = "jsonb")
    private String allowedModels;

    @Column(columnDefinition = "jsonb")
    private String deniedModels;

    @Column(columnDefinition = "jsonb")
    private String allowedIps;

    @Column(columnDefinition = "jsonb")
    private String rateLimit;

    @TableField(fill = FieldFill.INSERT)
    @Column(name = "quota_limit")
    private Long quotaLimit = 0L;

    @TableField(fill = FieldFill.INSERT)
    @Column(name = "quota_used")
    private Long quotaUsed = 0L;

    @TableField(fill = FieldFill.INSERT)
    @Column(name = "unlimited")
    private Boolean unlimited = false;

    @TableField(fill = FieldFill.INSERT)
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @TableField(fill = FieldFill.INSERT)
    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @TableField(fill = FieldFill.INSERT)
    @Column(name = "is_enabled")
    private Boolean isEnabled = true;

    @Column(length = 20)
    private String status = "active";

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public java.util.List<String> getAllowedModelsList() {
        try {
            if (allowedModels == null || allowedModels.isEmpty()) return new ArrayList<>();
            return objectMapper.readValue(allowedModels, new TypeReference<java.util.List<String>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public boolean isModelAllowed(String model) {
        java.util.List<String> denied = getDeniedModelsList();
        if (denied.contains(model)) return false;
        java.util.List<String> allowed = getAllowedModelsList();
        if (allowed.isEmpty() || allowed.contains("*")) return true;
        return allowed.contains(model);
    }

    public java.util.List<String> getDeniedModelsList() {
        try {
            if (deniedModels == null || deniedModels.isEmpty()) return new ArrayList<>();
            return objectMapper.readValue(deniedModels, new TypeReference<java.util.List<String>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public boolean isQuotaExceeded() {
        if (unlimited) return false;
        if (quotaLimit <= 0) return false;
        return quotaUsed >= quotaLimit;
    }

    public boolean isExpired() {
        if (expiresAt == null) return false;
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isValid() {
        return isEnabled != null && isEnabled && !isExpired() && !isQuotaExceeded() && "active".equals(status);
    }
}
