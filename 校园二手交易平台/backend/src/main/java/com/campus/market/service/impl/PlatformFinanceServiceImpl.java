package com.campus.market.service.impl;

import com.campus.market.entity.Order;
import com.campus.market.entity.PlatformAccount;
import com.campus.market.exception.BusinessException;
import com.campus.market.common.ResultCode;
import com.campus.market.mapper.OrderMapper;
import com.campus.market.mapper.PlatformAccountMapper;
import com.campus.market.service.PlatformFinanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlatformFinanceServiceImpl implements PlatformFinanceService {

    private final PlatformAccountMapper platformAccountMapper;
    private final OrderMapper            orderMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void escrowIn(Order order) {
        BigDecimal amt = order.getActualAmount();
        if (amt == null || amt.compareTo(BigDecimal.ZERO) < 0) {
            throw BusinessException.of(ResultCode.BAD_REQUEST, "订单实付金额异常");
        }
        if (amt.compareTo(BigDecimal.ZERO) == 0) {
            log.info("[托管入账] orderId={} 实付为 0（全额积分），跳过托管", order.getId());
            return;
        }
        int n = platformAccountMapper.addBalance(PlatformAccount.ESCROW_ID, "ESCROW", amt);
        if (n == 0) {
            throw BusinessException.of(ResultCode.INTERNAL_ERROR, "托管账户入账失败");
        }
        log.info("[托管入账] orderId={} escrow+ {}", order.getId(), amt);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void escrowOutToBuyerCancel(Order order) {
        debitEscrow(order.getActualAmount(), "取消订单退回买家");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void escrowOutToBuyerReturn(Order order) {
        debitEscrow(order.getActualAmount(), "退货同意退回买家");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void settleOrderCompleted(Order order) {
        BigDecimal actual = order.getActualAmount();
        BigDecimal merchantIncome = order.getMerchantIncome();
        BigDecimal platformFee = order.getPlatformFee();
        if (actual == null || merchantIncome == null || platformFee == null) {
            throw BusinessException.of(ResultCode.INTERNAL_ERROR, "订单金额字段缺失");
        }
        if (actual.compareTo(BigDecimal.ZERO) == 0) {
            int m = orderMapper.refundBalance(order.getMerchantId(), merchantIncome);
            if (m == 0) {
                throw BusinessException.of(ResultCode.INTERNAL_ERROR, "商家入账失败");
            }
            int f = platformAccountMapper.addBalance(PlatformAccount.FEE_ID, "FEE", platformFee);
            if (f == 0) {
                throw BusinessException.of(ResultCode.INTERNAL_ERROR, "平台手续费入账失败");
            }
            log.info("[订单结算] orderId={} 实付 0，跳过托管划转", order.getId());
            return;
        }
        int e = platformAccountMapper.deductBalance(PlatformAccount.ESCROW_ID, "ESCROW", actual);
        if (e == 0) {
            throw BusinessException.of(ResultCode.INTERNAL_ERROR,
                    "托管余额不足，无法结算 orderId=" + order.getId());
        }
        int m = orderMapper.refundBalance(order.getMerchantId(), merchantIncome);
        if (m == 0) {
            throw BusinessException.of(ResultCode.INTERNAL_ERROR, "商家入账失败");
        }
        int f = platformAccountMapper.addBalance(PlatformAccount.FEE_ID, "FEE", platformFee);
        if (f == 0) {
            throw BusinessException.of(ResultCode.INTERNAL_ERROR, "平台手续费入账失败");
        }
        log.info("[订单结算] orderId={} merchantId={} 商家+{} 手续费+{}",
                order.getId(), order.getMerchantId(), merchantIncome, platformFee);
    }

    private void debitEscrow(BigDecimal amt, String reason) {
        int n = platformAccountMapper.deductBalance(PlatformAccount.ESCROW_ID, "ESCROW", amt);
        if (n == 0) {
            throw BusinessException.of(ResultCode.INTERNAL_ERROR, "托管扣款失败：" + reason);
        }
        log.info("[托管出账] -{} ({})", amt, reason);
    }
}
