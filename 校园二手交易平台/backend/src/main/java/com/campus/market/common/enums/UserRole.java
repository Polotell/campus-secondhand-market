package com.campus.market.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 用户角色枚举
 * <p>{@code @EnumValue} 告诉 MyBatis Plus 用 {@link #code} 做 DB 存储；</p>
 * <p>{@code @JsonValue}  告诉 Jackson   用 {@link #code} 做前端 JSON 序列化。</p>
 */
public enum UserRole {

    /** 普通用户（买家 / 个人卖家） */
    USER("USER"),
    /** 商家（需要营业执照） */
    MERCHANT("MERCHANT"),
    /** 管理员 */
    ADMIN("ADMIN");

    @EnumValue
    @JsonValue
    private final String code;

    UserRole(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static UserRole of(String code) {
        for (UserRole r : values()) {
            if (r.code.equals(code)) return r;
        }
        return null;
    }
}
