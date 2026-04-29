package com.campus.market.vo;

import com.campus.market.common.enums.ReturnAuditStatus;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 退货申请视图（嵌入到 OrderDetailVO 或单独返回）
 */
@Data
public class ReturnRecordVO implements Serializable {

    private Long id;
    private Long orderId;
    private Long buyerId;
    private Long merchantId;

    private String reason;
    private String images;
    private LocalDateTime applyTime;

    private ReturnAuditStatus auditStatus;
    private String auditRemark;
    private LocalDateTime auditTime;
}
