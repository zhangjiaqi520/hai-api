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
import java.util.List;

@Data
@TableName("channels")
@Entity
@EntityListeners(AuditingEntityListener.class)
public class Channel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @TableId(type = IdType.AUTO)
    private Integer id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 50)
    private String type;

    @Column(name = "key_encrypted", nullable = false, columnDefinition = "TEXT")
    private String keyEncrypted;

    @Column(length = 500)
    private String baseUrl;

    @Column(columnDefinition = "jsonb")
    private String models;

    @TableField(fill = FieldFill.INSERT)
    @Column(name = "priority")
    private Integer priority = 0;

    @TableField(fill = FieldFill.INSERT)
    @Column(name = "weight")
    private Integer weight = 100;

    @TableField(fill = FieldFill.INSERT)
    @Column(name = "is_enabled")
    private Boolean isEnabled = true;

    @Column(length = 20)
    private String status = "active";

    @TableField(fill = FieldFill.INSERT)
    @Column(name = "last_test_at")
    private LocalDateTime lastTestAt;

    @TableField(fill = FieldFill.INSERT)
    @Column(name = "last_success_at")
    private LocalDateTime lastSuccessAt;

    @TableField(fill = FieldFill.INSERT)
    @Column(name = "avg_response_time")
    private Integer avgResponseTime = 0;

    @TableField(fill = FieldFill.INSERT)
    @Column(name = "success_rate", precision = 5, scale = 2)
    private Double successRate = 100.0;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public List<String> getModelsList() {
        try {
            if (models == null || models.isEmpty()) return new ArrayList<>();
            return objectMapper.readValue(models, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public boolean supportsModel(String model) {
        List<String> supportedModels = getModelsList();
        if (supportedModels.isEmpty() || supportedModels.contains("*")) return true;
        return supportedModels.contains(model);
    }
}
