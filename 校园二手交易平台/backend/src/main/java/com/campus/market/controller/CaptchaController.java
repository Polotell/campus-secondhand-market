package com.campus.market.controller;

import com.campus.market.common.Result;
import com.campus.market.utils.CaptchaUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 图形验证码接口（注册页使用）
 * <p>
 * 前端打开注册页时 GET 一次拿到 {@code captchaKey + imageBase64} 两个字段，
 * 提交表单时带上 captchaKey 和用户填写的 captchaCode，后端通过
 * {@link CaptchaUtil#verify(String, String)} 校验（一次性消费）。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/captcha")
public class CaptchaController {

    private final CaptchaUtil captchaUtil;

    @GetMapping
    public Result<CaptchaUtil.Captcha> get() {
        return Result.success(captchaUtil.generate());
    }
}
