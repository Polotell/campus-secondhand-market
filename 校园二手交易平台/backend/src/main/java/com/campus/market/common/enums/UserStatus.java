package com.campus.market.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

/** 用户审核 / 封禁状态 */
public enum UserStatus {

    /** 等待管理员审核 */
    PENDING("PENDING"),
    /** 审核通过（可登录使用） */
    APPROVED("APPROVED"),
    /** 审核拒绝 */
    REJECTED("REJECTED"),
    /** 已被封禁（可能带 ban_until 限时） */
    BANNED("BANNED");

    @EnumValue
    @JsonValue
    private final String code;

    UserStatus(String code) { this.code = code; }
    public String getCode() { return code; }
}
