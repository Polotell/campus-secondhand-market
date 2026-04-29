package com.campus.market.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 订单号生成器
 * <p>格式：{@code yyyyMMddHHmmss + 4 位毫秒 + 4 位随机数}，共 22 位。
 * <br>展示用，不承担唯一键作用；唯一性由 DB 自增/雪花 ID 保证。</p>
 */
public final class OrderNoGenerator {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private OrderNoGenerator() {}

    public static String next() {
        LocalDateTime now = LocalDateTime.now();
        String prefix = FMT.format(now);
        int ms = now.getNano() / 1_000_000;             // 0~999
        int rand = ThreadLocalRandom.current().nextInt(1000, 10000);
        return prefix + String.format("%03d", ms) + rand;
    }
}
