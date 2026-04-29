package com.campus.market.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单商品明细（带快照字段，防止商品后续改价/下架影响已成交订单）
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("order_item")
public class OrderItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long orderId;
    private Long productId;

    /** 商品快照 */
    private String productName;
    private String productImage;
    private BigDecimal unitPrice;

    private Integer quantity;
    private BigDecimal subtotal;

    /** 是否已评价（评价模块用） */
    private Integer reviewed;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
