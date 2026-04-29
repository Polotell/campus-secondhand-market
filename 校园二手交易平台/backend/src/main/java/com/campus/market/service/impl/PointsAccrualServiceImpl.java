package com.campus.market.service.impl;

import com.campus.market.entity.Order;
import com.campus.market.mapper.UserMapper;
import com.campus.market.service.PointsAccrualService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Service
@RequiredArgsConstructor
public class PointsAccrualServiceImpl implements PointsAccrualService {

    private final UserMapper userMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rewardBuyerOnOrderCompleted(Order order) {
        BigDecimal actual = order.getActualAmount();
        if (actual == null || actual.compareTo(BigDecimal.ZERO) <= 0) return;
        int pts = actual.setScale(0, RoundingMode.DOWN).intValue();
        if (pts <= 0) return;
        userMapper.addPoints(order.getBuyerId(), pts);
        log.info("[积分获赠] buyerId={} orderId={} +{} 分（实付 {} 元向下取整）",
                order.getBuyerId(), order.getId(), pts, actual);
    }
}
