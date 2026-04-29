package com.campus.market.common;

import com.campus.market.common.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 当前登录用户上下文（基于 ThreadLocal 存储）
 * <p>
 * 工作原理：
 * <ol>
 *   <li>{@code LoginInterceptor} 在 {@code preHandle} 中解析 JWT，把当前用户信息 {@code set()} 进 ThreadLocal；</li>
 *   <li>整个请求链（Controller → Service → Mapper）都可以通过 {@link #get()} 拿到当前用户；</li>
 *   <li>{@code afterCompletion} 中调用 {@link #clear()}，防止线程池复用导致的内存泄漏或串号。</li>
 * </ol>
 */
public final class UserContext {

    private static final ThreadLocal<Current> HOLDER = new ThreadLocal<>();

    private UserContext() {}

    public static void set(Current current) {
        HOLDER.set(current);
    }

    public static Current get() {
        return HOLDER.get();
    }

    public static Long getUserId() {
        Current c = HOLDER.get();
        return c == null ? null : c.getUserId();
    }

    public static UserRole getRole() {
        Current c = HOLDER.get();
        return c == null ? null : c.getRole();
    }

    public static String getUsername() {
        Current c = HOLDER.get();
        return c == null ? null : c.getUsername();
    }

    public static void clear() {
        HOLDER.remove();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Current {
        private Long     userId;
        private String   username;
        private UserRole role;
    }
}
