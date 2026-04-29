package com.campus.market.service;

import com.campus.market.common.PageResult;
import com.campus.market.dto.BlacklistAddDTO;
import com.campus.market.vo.UserBlacklistVO;

/**
 * 买家黑名单：平台级（全站不可买）或商家级（仅该店不可买）。
 */
public interface BlacklistService {

    /** 下单/加购前调用；命中则抛 {@link com.campus.market.common.ResultCode#BLACKLISTED} */
    void assertCanPurchase(Long buyerId, Long merchantId);

    void addByMerchant(Long merchantId, BlacklistAddDTO dto);

    void addByPlatform(Long adminId, BlacklistAddDTO dto);

    PageResult<UserBlacklistVO> listByMerchant(Long merchantId, long pageNum, long pageSize);

    PageResult<UserBlacklistVO> listPlatform(long pageNum, long pageSize);

    void removeByMerchant(Long merchantId, Long blacklistId);

    void removeByPlatform(Long blacklistId);
}
