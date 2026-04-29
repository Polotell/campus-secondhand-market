package com.campus.market.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 商品状态机
 * <pre>
 * DRAFT ─商家提交审核─> PENDING
 * PENDING ─管理员通过─> ON_SALE
 * PENDING ─管理员拒绝─> REJECTED
 * ON_SALE ─买家下单─>   LOCKED
 * LOCKED  ─订单完成─>   SOLD
 * ON_SALE ─商家下架─>   OFF_SHELF
 * OFF_SHELF ─商家上架─> ON_SALE
 * </pre>
 */
public enum ProductStatus {

    DRAFT("DRAFT"),
    PENDING("PENDING"),
    ON_SALE("ON_SALE"),
    LOCKED("LOCKED"),
    SOLD("SOLD"),
    OFF_SHELF("OFF_SHELF"),
    REJECTED("REJECTED");

    @EnumValue
    @JsonValue
    private final String code;

    ProductStatus(String code) { this.code = code; }
    public String getCode() { return code; }
}
