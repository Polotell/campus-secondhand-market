package com.campus.market.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 退货申请审核状态。
 * <p>与 {@code return_record.audit_status} 列对应。</p>
 */
public enum ReturnAuditStatus {

    /** 等待商家审核 */
    PENDING("PENDING"),
    /** 商家已同意（已退款 + 已还库存） */
    APPROVED("APPROVED"),
    /** 商家已拒绝（订单回到正常完结路径） */
    REJECTED("REJECTED");

    @EnumValue
    @JsonValue
    private final String code;

    ReturnAuditStatus(String code) { this.code = code; }
    public String getCode() { return code; }
}
