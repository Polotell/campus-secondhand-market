package com.campus.market.config;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.market.entity.User;
import com.campus.market.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 启动初始化（仅 dev profile 生效）
 * <p>
 * 职责：
 * <ol>
 *   <li>确保 {@code data.sql} 里预置的 admin / student01 / student02 / merchant01 账号密码
 *       是真实的 BCrypt("admin123")，避免 seed 里 hash 与当前 BCrypt 版本不一致导致登录失败。</li>
 * </ol>
 * <p>
 * 默认账号（全部明文密码 = <b>admin123</b>）：
 * <ul>
 *   <li>admin       / ADMIN    / APPROVED</li>
 *   <li>student01   / USER     / APPROVED</li>
 *   <li>student02   / USER     / APPROVED</li>
 *   <li>merchant01  / MERCHANT / APPROVED  等级 2（费率 0.2%）</li>
 * </ul>
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private static final String DEFAULT_PLAIN_PWD = "admin123";
    private static final String[] SEED_USERNAMES = {"admin", "student01", "student02", "merchant01"};

    /**
     * 开发期初始余额：学生默认 10000 元，方便下单测试；商家初始 0。
     * 注意：只在用户 balance = NULL 或 0 时补发，已充过值的账号不会被改动。
     */
    private static final Map<String, BigDecimal> SEED_BALANCE = Map.of(
            "student01",  new BigDecimal("10000.00"),
            "student02",  new BigDecimal("10000.00")
    );
    private static final Map<String, Integer> SEED_POINTS = Map.of(
            "student01",  5000,
            "student02",  5000
    );

    private final UserMapper userMapper;

    @Override
    public void run(String... args) {
        String freshHash = BCrypt.hashpw(DEFAULT_PLAIN_PWD);
        for (String u : SEED_USERNAMES) {
            User user = userMapper.selectOne(
                    new LambdaQueryWrapper<User>().eq(User::getUsername, u));
            if (user == null) continue;

            boolean changed = false;
            if (user.getPassword() == null || !BCrypt.checkpw(DEFAULT_PLAIN_PWD, user.getPassword())) {
                user.setPassword(freshHash);
                changed = true;
                log.info("[DataInitializer] 重置种子用户密码 -> admin123 : {}", u);
            }
            // 只在余额为空/0 时补发，已有余额说明跑过充值测试就别覆盖
            BigDecimal seed = SEED_BALANCE.get(u);
            if (seed != null
                    && (user.getBalance() == null || user.getBalance().compareTo(BigDecimal.ZERO) == 0)) {
                user.setBalance(seed);
                changed = true;
                log.info("[DataInitializer] 预置种子余额 {} = ¥{}", u, seed);
            }
            Integer seedPoints = SEED_POINTS.get(u);
            if (seedPoints != null && (user.getPoints() == null || user.getPoints() == 0)) {
                user.setPoints(seedPoints);
                changed = true;
                log.info("[DataInitializer] 预置种子积分 {} = {}", u, seedPoints);
            }
            if (changed) userMapper.updateById(user);
        }
    }
}
