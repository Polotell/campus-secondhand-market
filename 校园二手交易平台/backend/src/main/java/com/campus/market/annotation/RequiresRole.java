package com.campus.market.annotation;

import com.campus.market.common.enums.UserRole;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 角色权限注解（配合 {@code AuthAspect} AOP 切面生效）
 * <p>
 * 使用方式：
 * <pre>
 *   &#64;RequiresRole(UserRole.ADMIN)
 *   &#64;GetMapping("/admin/users")
 *   public Result&lt;?&gt; list() { ... }
 * </pre>
 * 未登录抛 401；角色不符抛 403。<b>（报告 4.4.3 AOP 权限校验示例）</b>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresRole {

    /** 允许访问的角色列表（OR 关系）；为空表示只要登录即可 */
    UserRole[] value() default {};
}
