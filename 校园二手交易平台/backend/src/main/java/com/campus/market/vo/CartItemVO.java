package com.campus.market.vo;

import com.campus.market.common.enums.ProductStatus;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 购物车单行视图，包含商品快照便于前端不再单独请求。
 */
@Data
public class CartItemVO implements Serializable {
    private Long id;
    private Long productId;

    private String  productName;
    private String  productImage;
    private BigDecimal unitPrice;
    private Integer stock;
    private ProductStatus productStatus;

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
