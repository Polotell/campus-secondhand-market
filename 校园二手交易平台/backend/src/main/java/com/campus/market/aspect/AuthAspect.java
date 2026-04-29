package com.campus.market.aspect;

import com.campus.market.annotation.RequiresRole;
import com.campus.market.common.ResultCode;
import com.campus.market.common.UserContext;
import com.campus.market.common.enums.UserRole;
import com.campus.market.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 角色权限 AOP 切面（【报告 4.5 关键代码展示】AOP 应用示例）
 * <p>
 * <b>IoC 原理：</b>类上有 {@code @Component}，Spring 容器扫描到后创建 Bean；
 * <b>AOP 原理：</b>类上有 {@code @Aspect}，Spring AOP 在容器启动时为匹配切点的 Bean 生成 CGLIB 动态代理；
 * 当调用被代理方法时，先进入环绕通知 {@link #around}，执行权限判断再决定是否继续。
 * <p>
 * 切入条件：方法（或类）上标注了 {@code @RequiresRole}。
 */
@Slf4j
@Aspect
@Component
public class AuthAspect {

    @Around("@annotation(com.campus.market.annotation.RequiresRole) " +
            "|| @within(com.campus.market.annotation.RequiresRole)")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        // ① 未登录
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw BusinessException.of(ResultCode.UNAUTHORIZED);
        }

        // ② 读注解（优先方法级，次之类级）
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        Method method = sig.getMethod();
        RequiresRole anno = AnnotationUtils.findAnnotation(method, RequiresRole.class);
        if (anno == null) {
            anno = AnnotationUtils.findAnnotation(method.getDeclaringClass(), RequiresRole.class);
        }

        // ③ 空数组 = 仅登录；非空 = 角色校验
        if (anno != null && anno.value().length > 0) {
            UserRole current = UserContext.getRole();
            boolean ok = false;
            for (UserRole allowed : anno.value()) {
                if (allowed == current) { ok = true; break; }
            }
            if (!ok) {
                log.warn("权限校验失败 uid={} role={} 需要={}", userId, current, anno.value());
                throw BusinessException.of(ResultCode.FORBIDDEN);
            }
        }
        return pjp.proceed();
    }
}
