package com.campus.market.dto;

import lombok.Data;

import javax.validation.constraints.*;

/**
 * 普通用户注册请求
 * <p>对应接口：POST /api/auth/register/user</p>
 */
@Data
public class UserRegisterDTO {

    @NotBlank(message = "验证码 key 不能为空")
    private String captchaKey;

    @NotBlank(message = "请输入验证码")
    @Size(min = 4, max = 6, message = "验证码长度不正确")
    private String captchaCode;

    @NotBlank(message = "请输入用户名")
    @Size(min = 4, max = 20, message = "用户名长度 4~20")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名仅支持字母、数字、下划线")
    private String username;

    @NotBlank(message = "请输入密码")
    @Size(min = 6, max = 32, message = "密码长度 6~32")
    private String password;

    @NotBlank(message = "请输入真实姓名")
    @Size(max = 50)
    private String realName;

    @NotBlank(message = "请输入手机号")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Email(message = "邮箱格式不正确")
    private String email;

    @Size(max = 50)
    private String city;

    @Pattern(regexp = "MALE|FEMALE|OTHER", message = "性别取值：MALE/FEMALE/OTHER")
    private String gender;

    @Pattern(regexp = "^\\d{16}$", message = "银行账号必须为 16 位数字")
    private String bankAccount;
}
