package com.campus.market.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 订单状态机（详见 /backend/doc/flow.md 2.4 节与 ER.md 4.3 节；主路径以 ReturnServiceImpl 为准）
 * <pre>
 * PAID → SHIPPED → RECEIVED →（可选）RETURN_APPLYING：同意 → RETURNED；拒绝 → 直接 COMPLETED（立即结算）
 *         └→ 无退货时 RECEIVED 超 return_deadline → 定时器结算 → COMPLETED
 * </pre>
 */
public enum OrderStatus {

    /** 已付款，货款在 ESCROW */
    PAID("PAID"),
    /** 商家已发货 */
    SHIPPED("SHIPPED"),
    /** 已收货（买家确认 or 7 天自动） */
    RECEIVED("RECEIVED"),
    /** 退货申请中 */
    RETURN_APPLYING("RETURN_APPLYING"),
    /** 商家同意退货 */
    RETURN_APPROVED("RETURN_APPROVED"),
    /** 商家拒绝退货 */
    RETURN_REJECTED("RETURN_REJECTED"),
    /** 退货完成（已退款） */
    RETURNED("RETURNED"),
    /** 交易完成（已结算给商家） */
    COMPLETED("COMPLETED"),
    /** 已取消 */
    CANCELLED("CANCELLED");

    @EnumValue
    @JsonValue
    private final String code;

    OrderStatus(String code) { this.code = code; }
    public String getCode() { return code; }
}
