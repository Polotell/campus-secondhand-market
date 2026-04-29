package com.campus.market.aspect;

import cn.hutool.core.util.StrUtil;
import com.campus.market.common.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * 接口访问日志切面（【报告 4.5 关键代码展示】AOP 应用示例 #2）
 * <p>
 * 切点：所有 {@code com.campus.market.controller} 包下的方法。
 * <br>通知类型：环绕通知（{@code @Around}），可在方法前后都织入逻辑。
 * <br>作用：打印接口 URI、当前用户、耗时；后续可扩展为写入 operation_log 表用于答辩演示。
 */
@Slf4j
@Aspect
@Component
public class LogAspect {

    @Around("execution(* com.campus.market.controller..*(..))")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.currentTimeMillis();
        String uri = currentUri();
        Long uid   = UserContext.getUserId();
        try {
            Object result = pjp.proceed();
            long cost = System.currentTimeMillis() - start;
            log.info("[API] uri={} uid={} method={} cost={}ms OK",
                    uri, uid, shortSig(pjp), cost);
            return result;
        } catch (Throwable e) {
            long cost = System.currentTimeMillis() - start;
            log.warn("[API] uri={} uid={} method={} cost={}ms FAIL: {}",
                    uri, uid, shortSig(pjp), cost, e.getMessage());
            throw e;
        }
    }

    private static String currentUri() {
        try {
            ServletRequestAttributes attr =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attr == null) return "-";
            HttpServletRequest req = attr.getRequest();
            return req.getMethod() + " " + req.getRequestURI();
        } catch (Exception ignore) { return "-"; }
    }

    private static String shortSig(ProceedingJoinPoint pjp) {
        String cls = pjp.getSignature().getDeclaringType().getSimpleName();
        return StrUtil.format("{}.{}()", cls, pjp.getSignature().getName());
    }
}
