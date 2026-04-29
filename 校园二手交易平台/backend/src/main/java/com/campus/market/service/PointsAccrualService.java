package com.campus.market.service;

import com.campus.market.entity.Order;

/**
 * 积分获赠：实验报告「1 元实付 = 1 积分」在订单完结（COMPLETED）时计入买家。
 */
public interface PointsAccrualService {

    void rewardBuyerOnOrderCompleted(Order order);
}
