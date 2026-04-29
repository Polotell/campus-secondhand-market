package com.campus.market.utils;

import cn.hutool.captcha.LineCaptcha;
import cn.hutool.core.lang.UUID;
import com.campus.market.common.ResultCode;
import com.campus.market.exception.BusinessException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 图形验证码工具（基于 Hutool 的 LineCaptcha）
 * <p>
 * 课程实验不引入 Redis，验证码暂存内存 ConcurrentHashMap；
 * <b>生产环境必须迁移到 Redis，带 TTL，否则多实例部署会出问题</b>。
 */
@Slf4j
@Component
public class CaptchaUtil {

    /** 验证码有效期（毫秒）：2 分钟 */
    private static final long TTL_MS = 2 * 60 * 1000L;

    /** key = 前端拿到的 captchaKey；value = {code, 过期时间} */
    private static final Map<String, CaptchaEntry> STORE = new ConcurrentHashMap<>();

    /** 生成一张新的验证码 */
    public Captcha generate() {
        LineCaptcha lc = cn.hutool.captcha.CaptchaUtil.createLineCaptcha(130, 48, 4, 20);
        String key  = UUID.fastUUID().toString(true);
        String code = lc.getCode();
        long exp    = Instant.now().toEpochMilli() + TTL_MS;
        STORE.put(key, new CaptchaEntry(code.toLowerCase(), exp));
        cleanExpired();
        Captcha c = new Captcha();
        c.setCaptchaKey(key);
        c.setImageBase64("data:image/png;base64," + lc.getImageBase64());
        return c;
    }

    /** 校验验证码，不通过直接抛业务异常 */
    public void verify(String key, String code) {
        if (key == null || code == null) {
            throw BusinessException.of(ResultCode.CAPTCHA_INVALID);
        }
        CaptchaEntry e = STORE.remove(key);   // 一次性使用
        if (e == null
                || e.getExpireAt() < Instant.now().toEpochMilli()
                || !e.getCode().equalsIgnoreCase(code)) {
            throw BusinessException.of(ResultCode.CAPTCHA_INVALID);
        }
    }

    /** 每 5 分钟定时清理一次过期验证码，避免长时间无访问时的内存堆积 */
    @Scheduled(fixedRate = 5 * 60 * 1000L)
    public void cleanExpired() {
        long now = Instant.now().toEpochMilli();
        int before = STORE.size();
        STORE.entrySet().removeIf(en -> en.getValue().getExpireAt() < now);
        int cleaned = before - STORE.size();
        if (cleaned > 0) {
            log.debug("[CaptchaUtil] 清理过期验证码 {} 条，剩余 {} 条", cleaned, STORE.size());
        }
    }

    @Data
    public static class Captcha {
        private String captchaKey;
        private String imageBase64;
    }

    @Data
    private static class CaptchaEntry {
        private final String code;
        private final long   expireAt;
    }
}
