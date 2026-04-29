package com.campus.market.interceptor;

import com.campus.market.common.Constants;
import com.campus.market.common.UserContext;
import com.campus.market.common.enums.UserRole;
import com.campus.market.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 登录拦截器
 * <p>
 * 职责：
 * <ol>
 *   <li>从请求头 {@code Authorization: Bearer xxx} 取 token；</li>
 *   <li>用 {@link JwtUtil} 解析，失败 → 401；</li>
 *   <li>成功 → 把当前用户信息塞进 {@link UserContext}，供 Controller/Service 取用；</li>
 *   <li>请求结束（afterCompletion）清理 ThreadLocal，防止线程池复用串号。</li>
 * </ol>
 * <p>
 * 白名单在 {@code WebMvcConfig} 里配置。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoginInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    @Value("${jwt.header}")
    private String headerName;

    @Value("${jwt.prefix}")
    private String headerPrefix;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // OPTIONS 预检请求直接放行，交给 CorsFilter
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String auth = request.getHeader(headerName);
        if (auth == null || !auth.startsWith(headerPrefix)) {
            writeUnauthorized(response, "未登录");
            return false;
        }
        String token = auth.substring(headerPrefix.length());
        Claims claims = jwtUtil.parse(token);
        if (claims == null) {
            writeUnauthorized(response, "Token 无效或已过期");
            return false;
        }

        Long   userId   = claims.get(Constants.JWT_CLAIM_USER_ID, Long.class);
        String username = claims.get(Constants.JWT_CLAIM_USERNAME, String.class);
        String roleStr  = claims.get(Constants.JWT_CLAIM_ROLE, String.class);
        UserContext.set(new UserContext.Current(userId, username, UserRole.of(roleStr)));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }

    private void writeUnauthorized(HttpServletResponse response, String msg) throws java.io.IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":401,\"message\":\"" + msg + "\",\"data\":null}");
    }
}
