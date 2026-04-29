package com.campus.market.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.market.common.enums.UserRole;
import com.campus.market.common.enums.UserStatus;
import com.campus.market.entity.MerchantReview;
import com.campus.market.entity.User;
import com.campus.market.mapper.MerchantReviewMapper;
import com.campus.market.mapper.OrderMapper;
import com.campus.market.mapper.UserMapper;
import com.campus.market.service.MerchantLevelAdjustService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * 动态等级规则（答辩可说明为「示例策略」，与 GMV、服务均分挂钩）：
 * <pre>
 * 5 级：GMV ≥ 50000 且 服务均分 ≥ 4.8
 * 4 级：GMV ≥ 20000 且 服务均分 ≥ 4.5
 * 3 级：GMV ≥ 5000  且 服务均分 ≥ 4.0
 * 2 级：GMV ≥ 1000  且 服务均分 ≥ 3.5
 * 1 级：其它
 * </pre>
 * 无评价记录时均分按 4.0 中性处理，避免新商家被锁死在 1 级。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantLevelAdjustServiceImpl implements MerchantLevelAdjustService {

    private final UserMapper           userMapper;
    private final OrderMapper          orderMapper;
    private final MerchantReviewMapper merchantReviewMapper;

    @Override
    @Scheduled(cron = "0 0 2 * * ?")
    public int runAdjustJob() {
        return doAdjust();
    }

    int doAdjust() {
        List<User> merchants = userMapper.selectList(new LambdaQueryWrapper<User>()
                .eq(User::getRole, UserRole.MERCHANT)
                .eq(User::getStatus, UserStatus.APPROVED));
        int changed = 0;
        for (User m : merchants) {
            BigDecimal gmv = orderMapper.sumCompletedGmvByMerchant(m.getId());
            List<MerchantReview> rs = merchantReviewMapper.selectList(
                    new LambdaQueryWrapper<MerchantReview>().eq(MerchantReview::getMerchantId, m.getId()));
            double avg = 4.0;
            if (!rs.isEmpty()) {
                avg = rs.stream().mapToInt(MerchantReview::getRating).average().orElse(4.0);
            }
            int level = resolveLevel(gmv, avg);
            if (m.getMerchantLevel() == null || m.getMerchantLevel() != level) {
                m.setMerchantLevel(level);
                userMapper.updateById(m);
                changed++;
                log.info("[商家等级调整] uid={} shop={} -> {} 级 (GMV={} 服务均分≈{})",
                        m.getId(), m.getShopName(), level, gmv, String.format("%.2f", avg));
            }
        }
        if (changed > 0) {
            log.info("[商家等级定时任务] 共调整 {} 个商家", changed);
        }
        return changed;
    }

    private static int resolveLevel(BigDecimal gmv, double avgSvc) {
        double g = gmv == null ? 0 : gmv.doubleValue();
        if (g >= 50_000 && avgSvc >= 4.8) return 5;
        if (g >= 20_000 && avgSvc >= 4.5) return 4;
        if (g >= 5_000 && avgSvc >= 4.0) return 3;
        if (g >= 1_000 && avgSvc >= 3.5) return 2;
        return 1;
    }
}
