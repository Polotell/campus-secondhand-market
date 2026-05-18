package com.campus.market.controller;

import com.campus.market.annotation.RequiresRole;
import com.campus.market.common.Result;
import com.campus.market.dto.LoginDTO;
import com.campus.market.dto.MerchantRegisterDTO;
import com.campus.market.dto.UserRegisterDTO;
import com.campus.market.service.AuthService;
import com.campus.market.service.UserService;
import com.campus.market.vo.LoginVO;
import com.campus.market.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Map;

/**
 * 认证模块 Controller（/api/auth 开头）
 * <p>
 * 白名单接口（无需登录，已在 {@code WebMvcConfig} 里放行）：
 * <ul>
 *   <li>POST /auth/register/user       —— 普通用户注册</li>
 *   <li>POST /auth/register/merchant   —— 商家注册</li>
 *   <li>POST /auth/login               —— 登录</li>
 * </ul>
 * 需要登录：
 * <ul>
 *   <li>GET  /auth/me                  —— 获取当前登录用户</li>
 *   <li>POST /auth/logout              —— 登出（JWT 无状态，前端删 token 即可；服务端这里仅做预留）</li>
 * </ul>
 */
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    @PostMapping("/register/user")
    public Result<Map<String, String>> registerUser(@RequestBody @Valid UserRegisterDTO dto) {
        Long id = authService.registerUser(dto);
        // userId 用字符串返给前端，避免雪花 Long 被 JSON 当成 Number 导致 JS 精度丢失
        return Result.success(Map.of("userId", String.valueOf(id)), "注册成功，请等待管理员审核");
    }

    @PostMapping("/register/merchant")
    public Result<Map<String, String>> registerMerchant(@RequestBody @Valid MerchantRegisterDTO dto) {
        Long id = authService.registerMerchant(dto);
        return Result.success(Map.of("userId", String.valueOf(id)), "商家注册成功，请等待管理员审核");
    }

    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody @Valid LoginDTO dto) {
        return Result.success(authService.login(dto), "登录成功");
    }

    @GetMapping("/me")
    @RequiresRole   // 仅要求登录
    public Result<UserVO> me() {
        return Result.success(UserVO.from(userService.getCurrentOrThrow()));
    }

    @PostMapping("/logout")
    @RequiresRole
    public Result<Void> logout() {
        // JWT 无状态：实际登出由前端删除本地 token 完成；
        // 如果后续要支持"服务端强制登出"，需改为 Redis 黑名单实现。
        return Result.success();
    }
}
