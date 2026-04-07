package com.haiapi.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Data
@TableName("usage_logs")
@Entity
@EntityListeners(AuditingEntityListener.class)
public class UsageLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField(fill = FieldFill.INSERT)
    @Column(name = "token_id", nullable = false)
    private String tokenId;

    @TableField(fill = FieldFill.INSERT)
    @Column(name = "channel_id")
    private Integer channelId;

    @Column(nullable = false, length = 100)
    private String model;

    @TableField(fill = FieldFill.INSERT)
    @Column(name = "input_tokens")
    private Integer inputTokens = 0;

    @TableField(fill = FieldFill.INSERT)
    @Column(name = "output_tokens")
    private Integer outputTokens = 0;

    @TableField(fill = FieldFill.INSERT)
    @Column(name = "response_time_ms")
    private Integer responseTimeMs = 0;

    @TableField(fill = FieldFill.INSERT)
    @Column(name = "status_code")
    private Integer statusCode;

    @Column(name = "error_type", length = 50)
    private String errorType;

    @Column(name = "ip_address")
    private String ipAddress;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public int getTotalTokens() {
        return (inputTokens != null ? inputTokens : 0) + (outputTokens != null ? outputTokens : 0);
    }

    public boolean isSuccess() {
        return statusCode != null && statusCode >= 200 && statusCode < 300;
    }
}
