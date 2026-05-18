package com.campus.market.vo;

import com.campus.market.common.enums.ProductStatus;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 购物车单行视图，包含商品快照便于前端不再单独请求。
 */
@Data
public class CartItemVO implements Serializable {
    /** JSON 输出为字符串，避免前端 JS Number 精度丢失导致结算时报「购物车条目不存在」 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long productId;

    private String  productName;
    private String  productImage;
    private BigDecimal unitPrice;
    private Integer stock;
    private ProductStatus productStatus;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long   merchantId;
    private String shopName;

    private Integer quantity;
    private Integer selected;

    private BigDecimal subtotal;

    /** 当前商品是否可结算（下架 / 被删 / 库存不足等） */
    private Boolean available;
    /** 不可结算的原因 */
    private String  unavailableReason;

    private LocalDateTime createdAt;
}
