package com.campus.market.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.market.common.PageResult;
import com.campus.market.common.ResultCode;
import com.campus.market.dto.BlacklistAddDTO;
import com.campus.market.entity.User;
import com.campus.market.entity.UserBlacklist;
import com.campus.market.exception.BusinessException;
import com.campus.market.mapper.UserBlacklistMapper;
import com.campus.market.mapper.UserMapper;
import com.campus.market.service.BlacklistService;
import com.campus.market.vo.UserBlacklistVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BlacklistServiceImpl implements BlacklistService {

    private final UserBlacklistMapper userBlacklistMapper;
    private final UserMapper          userMapper;

    @Override
    public void assertCanPurchase(Long buyerId, Long merchantId) {
        if (buyerId == null || merchantId == null) return;
        int n = userBlacklistMapper.countBlockPurchase(buyerId, merchantId);
        if (n > 0) {
            throw BusinessException.of(ResultCode.BLACKLISTED);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addByMerchant(Long merchantId, BlacklistAddDTO dto) {
        if (dto.getUserId().equals(merchantId)) {
            throw BusinessException.of(ResultCode.BAD_REQUEST, "不能拉黑自己");
        }
        User buyer = userMapper.selectById(dto.getUserId());
        if (buyer == null) {
            throw BusinessException.of(ResultCode.NOT_FOUND, "用户不存在");
        }
        long dup = userBlacklistMapper.selectCount(new LambdaQueryWrapper<UserBlacklist>()
                .eq(UserBlacklist::getUserId, dto.getUserId())
                .eq(UserBlacklist::getMerchantId, merchantId));
        if (dup > 0) {
            throw BusinessException.of(ResultCode.BAD_REQUEST, "已在该店铺黑名单中");
        }
        UserBlacklist b = new UserBlacklist();
        b.setUserId(dto.getUserId());
        b.setMerchantId(merchantId);
        b.setReason(dto.getReason());
        b.setOperatorId(merchantId);
        userBlacklistMapper.insert(b);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addByPlatform(Long adminId, BlacklistAddDTO dto) {
        User buyer = userMapper.selectById(dto.getUserId());
        if (buyer == null) {
            throw BusinessException.of(ResultCode.NOT_FOUND, "用户不存在");
        }
        long dup = userBlacklistMapper.selectCount(new LambdaQueryWrapper<UserBlacklist>()
                .eq(UserBlacklist::getUserId, dto.getUserId())
                .isNull(UserBlacklist::getMerchantId));
        if (dup > 0) {
            throw BusinessException.of(ResultCode.BAD_REQUEST, "已在全平台黑名单中");
        }
        UserBlacklist b = new UserBlacklist();
        b.setUserId(dto.getUserId());
        b.setMerchantId(null);
        b.setReason(dto.getReason());
        b.setOperatorId(adminId);
        userBlacklistMapper.insert(b);
    }

    @Override
    public PageResult<UserBlacklistVO> listByMerchant(Long merchantId, long pageNum, long pageSize) {
        Page<UserBlacklist> page = userBlacklistMapper.selectPage(Page.of(pageNum, pageSize),
                new LambdaQueryWrapper<UserBlacklist>()
                        .eq(UserBlacklist::getMerchantId, merchantId)
                        .orderByDesc(UserBlacklist::getCreatedAt));
        return toPage(page);
    }

    @Override
    public PageResult<UserBlacklistVO> listPlatform(long pageNum, long pageSize) {
        Page<UserBlacklist> page = userBlacklistMapper.selectPage(Page.of(pageNum, pageSize),
                new LambdaQueryWrapper<UserBlacklist>()
                        .isNull(UserBlacklist::getMerchantId)
                        .orderByDesc(UserBlacklist::getCreatedAt));
        return toPage(page);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeByMerchant(Long merchantId, Long blacklistId) {
        UserBlacklist b = userBlacklistMapper.selectById(blacklistId);
        if (b == null) throw BusinessException.of(ResultCode.NOT_FOUND, "记录不存在");
        if (b.getMerchantId() == null || !b.getMerchantId().equals(merchantId)) {
            throw BusinessException.of(ResultCode.FORBIDDEN);
        }
        userBlacklistMapper.deleteById(blacklistId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeByPlatform(Long blacklistId) {
        UserBlacklist b = userBlacklistMapper.selectById(blacklistId);
        if (b == null) throw BusinessException.of(ResultCode.NOT_FOUND, "记录不存在");
        if (b.getMerchantId() != null) {
            throw BusinessException.of(ResultCode.BAD_REQUEST, "非平台级黑名单，请由商家解除");
        }
        userBlacklistMapper.deleteById(blacklistId);
    }

    private PageResult<UserBlacklistVO> toPage(Page<UserBlacklist> page) {
        List<UserBlacklist> rows = page.getRecords();
        if (rows.isEmpty()) return PageResult.of(page, List.of());
        Set<Long> uids = rows.stream().map(UserBlacklist::getUserId).collect(Collectors.toSet());
        Set<Long> mids = rows.stream().map(UserBlacklist::getMerchantId).filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        uids.addAll(mids);
        Map<Long, User> uMap = userMapper.selectBatchIds(uids).stream()
                .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));
        List<UserBlacklistVO> vos = rows.stream().map(r -> {
            UserBlacklistVO v = new UserBlacklistVO();
            v.setId(r.getId());
            v.setUserId(r.getUserId());
            User bu = uMap.get(r.getUserId());
            v.setBuyerName(bu == null ? "-" : StrUtil.blankToDefault(bu.getRealName(), bu.getUsername()));
            v.setMerchantId(r.getMerchantId());
            if (r.getMerchantId() != null) {
                User m = uMap.get(r.getMerchantId());
                v.setShopName(m == null ? "-" : StrUtil.blankToDefault(m.getShopName(), m.getUsername()));
            } else {
                v.setShopName("全平台");
            }
            v.setReason(r.getReason());
            v.setOperatorId(r.getOperatorId());
            v.setCreatedAt(r.getCreatedAt());
            return v;
        }).collect(Collectors.toList());
        return PageResult.of(page, vos);
    }
}
