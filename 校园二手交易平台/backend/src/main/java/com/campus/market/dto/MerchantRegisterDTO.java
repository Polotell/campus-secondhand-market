package com.campus.market.dto;

import lombok.Data;

import javax.validation.constraints.*;

/**
 * 商家注册请求
 * <p>对应接口：POST /api/auth/register/merchant</p>
 * <p>与普通用户的差异：必填店铺名、营业执照、身份证正反面。图片字段由前端先调
 * {@code /api/file/upload} 上传后拿到 URL，再提交到此 DTO。</p>
 */
@Data
public class MerchantRegisterDTO {

    @NotBlank(message = "验证码 key 不能为空")
    private String captchaKey;
    @NotBlank(message = "请输入验证码")
    private String captchaCode;

    @NotBlank(message = "请输入用户名")
    @Size(min = 4, max = 20)
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名仅支持字母、数字、下划线")
    private String username;

    @NotBlank
    @Size(min = 6, max = 32)
    private String password;

    @NotBlank(message = "请输入姓名（法人/负责人）")
    private String realName;

    @NotBlank
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Email
    private String email;

    @Size(max = 50)
    private String city;

    @Pattern(regexp = "MALE|FEMALE|OTHER", message = "性别取值：MALE/FEMALE/OTHER")
    private String gender;

    @NotBlank(message = "请输入银行账号")
    @Pattern(regexp = "^\\d{16}$", message = "银行账号必须为 16 位数字")
    private String bankAccount;

    @NotBlank(message = "请输入店铺名称")
    @Size(max = 100)
    private String shopName;

    @NotBlank(message = "请上传营业执照")
    private String businessLicense;

    @NotBlank(message = "请上传身份证正面照")
    private String idCardFront;

    @NotBlank(message = "请上传身份证反面照")
    private String idCardBack;
}
