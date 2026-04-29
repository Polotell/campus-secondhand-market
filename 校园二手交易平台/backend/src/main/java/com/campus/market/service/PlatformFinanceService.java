package com.campus.market.service;

import com.campus.market.entity.Order;

import java.math.BigDecimal;

/**
 * 平台资金：托管账户 ESCROW + 手续费账户 FEE。
 * <p>与实验报告「货款进中间账户、确认后打给卖家」一致：
 * 买家下单扣款成功后，等额进入 ESCROW；订单完结（COMPLETED）时拆给商家余额与 FEE；
 * 取消/退货同意则从 ESCROW 退回买家。</p>
 */
public interface PlatformFinanceService {

    /** 下单成功：买家已扣款，等额记入托管 */
    void escrowIn(Order order);

    /** 买家取消（仅 PAID）：从托管退回买家（调用方已负责 refundBalance）— 本方法只扣 ESCROW */
    void escrowOutToBuyerCancel(Order order);

    /** 退货同意：托管资金退回买家（调用方已 refundBalance） */
    void escrowOutToBuyerReturn(Order order);

    /**
     * 订单完结：RECEIVED→COMPLETED 或 退货被拒→COMPLETED。
     * 从 ESCROW 划出 actualAmount = merchantIncome + platformFee，分别入账商家与手续费账户。
     */
    void settleOrderCompleted(Order order);
}
