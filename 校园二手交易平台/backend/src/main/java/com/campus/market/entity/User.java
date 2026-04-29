package com.campus.market.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.campus.market.common.enums.UserRole;
import com.campus.market.common.enums.UserStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户实体（对应表 user，承载三种角色：普通用户 / 商家 / 管理员）
 * <p>
 * 注意：
 * <ul>
 *   <li>{@code password} 字段加 {@link JsonIgnore}，永远不会被序列化给前端。</li>
 *   <li>{@code role / status} 用自定义枚举，通过 MyBatis Plus 的 {@code @EnumValue} 自动转换。</li>
 *   <li>{@code createdAt / updatedAt} 由 {@code MyMetaObjectHandler} 自动填充，业务代码不需要手动 set。</li>
 * </ul>
 */
@Data
@TableName("`user`")   // user 是 MySQL 关键字，加反引号
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String username;

    /** BCrypt 加密后的密码；禁止返回给前端 */
    @JsonIgnore
    private String password;

    private String realName;
    private String phone;
    private String email;
    private String city;
    private String gender;
    private String bankAccount;
    private String avatar;

    private UserRole   role;
    private UserStatus status;

    private String rejectReason;
    private LocalDateTime banUntil;

    // ==================== 商家字段 ====================
    private String shopName;
    private String businessLicense;
    private String idCardFront;
    private String idCardBack;
    /** 商家等级 1~5，对应费率 0.1%~1% */
    private Integer merchantLevel;
    /** 商家好评率（0~1） */
    private BigDecimal goodRate;
    /** 作为买家的好评率（商家对买家评价聚合） */
    private BigDecimal buyerGoodRate;

    // ==================== 钱包与积分 ====================
    private BigDecimal balance;
    private Integer    points;

    // ==================== 通用字段 ====================
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    @JsonIgnore
    private Integer deleted;
}
