package com.campus.market.vo;

import com.campus.market.common.enums.OrderStatus;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单详情（含所有订单项）
 */
@Data
public class OrderDetailVO implements Serializable {
    private Long id;
    private String orderNo;

    private Long   merchantId;
    private String shopName;

    private Long   buyerId;
    private String buyerName;

    private BigDecimal totalAmount;
    private Integer pointsUsed;
    private BigDecimal pointsDeduction;
    private BigDecimal actualAmount;
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
    private LocalDateTime createdAt;

    private String remark;
    private String meetPlace;
    private String meetTime;

    private List<OrderItemVO> items;

    /**
     * 退货申请（如有）；状态可能是 PENDING / APPROVED / REJECTED。
     * 若为 null，表示订单从未被申请过退货。
     */
    private ReturnRecordVO returnRecord;

    /**
     * 当前买家是否还能发起退货申请。
     * <p>判定条件：状态 = RECEIVED 且 returnRecord 为空 且 returnDeadline > NOW。
     * 后端在 {@code OrderServiceImpl.assembleDetail} 中按当前角色填充。</p>
     */
    private Boolean canApplyReturn;

    @Data
    public static class OrderItemVO implements Serializable {
        private Long id;
        private Long productId;
        private String productName;
        private String productImage;
        private BigDecimal unitPrice;
        private Integer quantity;
        private BigDecimal subtotal;
        private Integer reviewed;
    }
}
