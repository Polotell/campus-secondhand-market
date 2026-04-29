package com.campus.market.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 操作日志（管理员重要动作 + AOP 切面自动记录）
 * <p>对应表 {@code operation_log}。用途：
 * <ul>
 *   <li>管理员审核/封禁/驳回 等动作的"可追溯证据"</li>
 *   <li>报告 4.5 展示 AOP 环绕通知 + 4.6 展示业务审计能力</li>
 * </ul>
 */
@Data
@Builder
@TableName("operation_log")
public class OperationLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long   userId;
    private String username;
    private String module;
    private String operation;
    private String requestUri;
    private String requestMethod;
    private String params;
    private String ip;
    private Integer durationMs;
    private String status;        // SUCCESS / FAIL
    private String errorMsg;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
