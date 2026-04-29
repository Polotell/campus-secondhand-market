package com.campus.market.service;

/**
 * 按交易额 + 服务态度（买家对商家的 merchant_review）动态调整商家等级。
 * <p>实验报告必选（5）：与管理员手工调级并存，每日定时重算。</p>
 */
public interface MerchantLevelAdjustService {

    /** @return 本轮调整人数的近似值（有变更则计数） */
    int runAdjustJob();
}
