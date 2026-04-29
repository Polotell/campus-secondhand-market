package com.campus.market.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.campus.market.common.enums.ReturnAuditStatus;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 退货申请记录。
 * <p>每个订单同一时刻最多一条有效记录（{@code uk_order(order_id, deleted)}）。
 * 拒绝后视作"申请关闭"，订单回到正常完结路径；不允许重复申请。</p>
 */
@Data
@TableName("return_record")
public class ReturnRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long orderId;
    private Long buyerId;
    private Long merchantId;

    private String reason;
    /** 凭证图（JSON 数组字符串）；v1 简化，前端可空 */
    private String images;

    private LocalDateTime applyTime;

    private ReturnAuditStatus auditStatus;
    private String auditRemark;
    private LocalDateTime auditTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
