package com.campus.market.utils;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 商家等级 → 平台手续费率映射
 * <p>与需求文档保持一致，等级越高费率越低。</p>
 */
public final class MerchantFeeRate {

    /** 默认 0.5%（非商家或等级异常时兜底） */
    public static final BigDecimal DEFAULT = new BigDecimal("0.0050");

    private static final Map<Integer, BigDecimal> TABLE = Map.of(
            1, new BigDecimal("0.0050"), // 1 级 0.5%
            2, new BigDecimal("0.0020"), // 2 级 0.2%
            3, new BigDecimal("0.0010"), // 3 级 0.1%
            4, new BigDecimal("0.0005"), // 4 级 0.05%
            5, new BigDecimal("0.0000")  // 5 级 0%
    );

    private MerchantFeeRate() {}

    public static BigDecimal rateOf(Integer level) {
        if (level == null) return DEFAULT;
        return TABLE.getOrDefault(level, DEFAULT);
    }
}
