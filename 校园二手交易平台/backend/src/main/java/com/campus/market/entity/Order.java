package com.campus.market.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.campus.market.common.enums.OrderStatus;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单主表
 * <p>业务规则（详见 /backend/doc/flow.md）：</p>
 * <ul>
 *   <li>一订单一商家（购物车按商家拆单）</li>
 *   <li>下单 = 扣买家余额 + 扣库存 + 写订单，原子事务</li>
 *   <li>完成 = 扣平台手续费 + 记商家应得（v1 不做结算给商家，v2 补）</li>
 * </ul>
 */
@Data
@TableName("`order`")
public class Order implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String orderNo;
    private Long buyerId;
    private Long merchantId;

    private BigDecimal totalAmount;
    private Integer    pointsUsed;
    private BigDecimal pointsDeduction;
    private BigDecimal actualAmount;

    /** 下单时商家等级对应费率快照；避免后续商家升级后老订单重算 */
    private BigDecimal platformFeeRate;
    private BigDecimal platformFee;
    private BigDecimal merchantIncome;

    private OrderStatus status;

    private LocalDateTime paidAt;
    private LocalDateTime shippedAt;
    private LocalDateTime receivedAt;
    private LocalDateTime autoConfirmAt;
    private LocalDateTime returnDeadline;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;

    private String remark;

    /** 线下交易约定地点（实验报告：约定时间地点） */
    private String meetPlace;
    /** 线下交易约定时间（自由文本，如「周五下午 3 点」） */
    private String meetTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
