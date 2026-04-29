package com.campus.market.vo;

import com.campus.market.common.enums.OrderStatus;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单列表项（不含完整订单项；仅取头 3 条做预览）
 */
@Data
public class OrderListVO implements Serializable {
    private Long id;
    private String orderNo;

    private Long   merchantId;
    private String shopName;

    private Long   buyerId;
    private String buyerName;

    private BigDecimal totalAmount;
    private BigDecimal actualAmount;
    private OrderStatus status;

    private Integer itemCount;        // 所有商品合计件数
    private List<OrderItemSnapshot> items; // 最多 3 条预览

    private LocalDateTime createdAt;
    private LocalDateTime paidAt;

    @Data
    public static class OrderItemSnapshot implements Serializable {
        private Long   productId;
        private String productName;
        private String productImage;
        private BigDecimal unitPrice;
        private Integer quantity;
    }
}
