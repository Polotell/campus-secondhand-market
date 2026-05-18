package com.campus.market.vo;

import com.campus.market.common.enums.ReturnAuditStatus;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 退货申请视图（嵌入到 OrderDetailVO 或单独返回）
 */
@Data
public class ReturnRecordVO implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long orderId;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long buyerId;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long merchantId;

    private String reason;
    private String images;
    private LocalDateTime applyTime;

    private ReturnAuditStatus auditStatus;
    private String auditRemark;
    private LocalDateTime auditTime;
}
