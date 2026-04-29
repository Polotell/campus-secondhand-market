package com.campus.market.service;

import com.campus.market.common.PageResult;
import com.campus.market.common.enums.OrderStatus;
import com.campus.market.dto.CheckoutDTO;
import com.campus.market.vo.OrderDetailVO;
import com.campus.market.vo.OrderListVO;

import java.math.BigDecimal;
import java.util.List;

public interface OrderService {

    /**
     * 结算预览（只读）：把所选购物车条目聚合成"预下单"对象；
     * 不做任何写操作，便于前端展示金额明细与可用性。
     */
    Preview preview(Long buyerId, List<Long> cartItemIds);

    /**
     * 核心：下单（原子事务）
     * <ol>
     *   <li>校验购物车权属 + 商品可售 + 同一商家 + 不买自己</li>
     *   <li>乐观原子 SQL 扣库存 & 扣余额，失败回滚</li>
     *   <li>写 order + order_item + 清购物车</li>
     * </ol>
     * @return 新订单 id
     */
    Long create(Long buyerId, CheckoutDTO dto);

    /** 买家：我的订单列表 */
    PageResult<OrderListVO> listByBuyer(Long buyerId, OrderStatus status, long pageNum, long pageSize);

    /** 买家：订单详情（仅本人可看） */
    OrderDetailVO detailForBuyer(Long buyerId, Long orderId);

    /**
     * 买家取消订单（仅 PAID 状态可取消）
     * 会把钱退回买家 + 归还库存。
     */
    void cancelByBuyer(Long buyerId, Long orderId);

    /**
     * 买家确认收货（SHIPPED → RECEIVED）。
     * 同时记录 {@code received_at} 与 {@code return_deadline = received_at + 24h}；
     * 24h 内可申请退货，超期由定时任务自动完结。
     */
    void confirmReceive(Long buyerId, Long orderId);

    // ============== 商家侧 ==============

    /** 商家：我的店铺订单列表（按状态过滤，按下单时间倒序） */
    PageResult<OrderListVO> listByMerchant(Long merchantId, OrderStatus status,
                                           long pageNum, long pageSize);

    /** 商家：订单详情（仅本店铺订单可见） */
    OrderDetailVO detailForMerchant(Long merchantId, Long orderId);

    /**
     * 商家发货（PAID → SHIPPED）。
     * 设置 {@code shipped_at} 与 {@code auto_confirm_at = shipped_at + 7天}。
     */
    void shipByMerchant(Long merchantId, Long orderId);

    // ============== 后台定时任务 ==============

    /**
     * 7 天未确认收货的订单：SHIPPED → RECEIVED；
     * 同时记录 {@code received_at} 与 {@code return_deadline = received_at + 24h}。
     * @return 本轮处理的订单数
     */
    int autoConfirmReceiveJob();

    /**
     * 24h 退货窗口过期的订单：RECEIVED → COMPLETED；
     * 托管资金结算给商家 + 平台手续费，并发积分；并设置 {@code completed_at}。
     * @return 本轮处理的订单数
     */
    int autoCompleteJob();

    @lombok.Data
    class Preview {
        private Long   merchantId;
        private String shopName;
        private List<com.campus.market.vo.CartItemVO> items;
        private BigDecimal totalAmount;   // 商品合计
        private Integer pointsUsable;     // 最多可抵扣的积分（v1 = 0）
        private BigDecimal pointsDeduction = BigDecimal.ZERO;
        private BigDecimal actualAmount;  // 实付
        private BigDecimal buyerBalance;  // 买家余额（展示用）
    }
}
