package com.campus.market.controller;

import com.campus.market.annotation.RequiresRole;
import com.campus.market.common.Result;
import com.campus.market.common.UserContext;
import com.campus.market.common.enums.UserRole;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Hello 测试接口（验证工程骨架跑通用）
 * <p>三个端点对应三种场景：<ul>
 *   <li>{@code GET /api/hello/ping} —— 匿名访问（走白名单）</li>
 *   <li>{@code GET /api/hello/me}   —— 需登录</li>
 *   <li>{@code GET /api/hello/admin}—— 需管理员</li>
 * </ul></p>
 */
@RestController
@RequestMapping("/hello")
public class HelloController {

    @GetMapping("/ping")
    public Result<Map<String, Object>> ping() {
        return Result.success(Map.of(
                "app", "campus-market",
                "time", LocalDateTime.now(),
                "message", "pong"
        ));
    }

    @GetMapping("/me")
    @RequiresRole   // 仅要求登录，不限角色
    public Result<UserContext.Current> me() {
        return Result.success(UserContext.get());
    }

    @GetMapping("/admin")
    @RequiresRole(UserRole.ADMIN)
    public Result<String> admin() {
        return Result.success("欢迎管理员 " + UserContext.get().getUsername());
    }
}
