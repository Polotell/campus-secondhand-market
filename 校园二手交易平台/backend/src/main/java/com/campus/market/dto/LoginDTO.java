package com.campus.market.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 登录请求
 * <p>对应接口：POST /api/auth/login</p>
 */
@Data
public class LoginDTO {

    @NotBlank(message = "请输入用户名")
    private String username;

    @NotBlank(message = "请输入密码")
    private String password;
}
