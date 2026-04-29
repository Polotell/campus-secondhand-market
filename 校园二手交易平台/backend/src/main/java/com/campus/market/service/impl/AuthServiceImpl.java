package com.campus.market.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.market.common.ResultCode;
import com.campus.market.common.enums.UserRole;
import com.campus.market.common.enums.UserStatus;
import com.campus.market.dto.LoginDTO;
import com.campus.market.dto.MerchantRegisterDTO;
import com.campus.market.dto.UserRegisterDTO;
import com.campus.market.entity.User;
import com.campus.market.exception.BusinessException;
import com.campus.market.mapper.UserMapper;
import com.campus.market.service.AuthService;
import com.campus.market.utils.CaptchaUtil;
import com.campus.market.utils.JwtUtil;
import com.campus.market.vo.LoginVO;
import com.campus.market.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 认证业务实现
 * <p>
 * <b>Spring IoC：</b>类上加 {@code @Service}，Spring 启动时扫描并实例化该 Bean，通过构造器注入
 * {@link UserMapper}、{@link CaptchaUtil}、{@link JwtUtil}，体现控制反转（IoC）和依赖注入（DI）。
 * <p>
 * <b>事务控制：</b>注册方法加 {@code @Transactional(rollbackFor = Exception.class)}，
 * 保证"校验验证码 → 查重 → insert"任一环节异常时数据库回滚，避免脏数据。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper   userMapper;
    private final CaptchaUtil  captchaUtil;
    private final JwtUtil      jwtUtil;

    @Value("${jwt.prefix}")
    private String tokenPrefix;

    // ==================== 普通用户注册 ====================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long registerUser(UserRegisterDTO dto) {
        // 1. 校验验证码（不通过直接抛 10003）
        captchaUtil.verify(dto.getCaptchaKey(), dto.getCaptchaCode());

        // 2. 用户名查重
        assertUsernameFree(dto.getUsername());

        // 3. 构造实体并插入
        User u = new User();
        u.setUsername(dto.getUsername());
        u.setPassword(BCrypt.hashpw(dto.getPassword()));
        u.setRealName(dto.getRealName());
        u.setPhone(dto.getPhone());
        u.setEmail(dto.getEmail());
        u.setCity(dto.getCity());
        u.setGender(dto.getGender());
        u.setBankAccount(dto.getBankAccount());
        u.setRole(UserRole.USER);
        u.setStatus(UserStatus.PENDING);      // 注册后待管理员审核
        u.setBalance(BigDecimal.ZERO);
        u.setPoints(0);
        userMapper.insert(u);

        log.info("新用户注册待审核 id={} username={}", u.getId(), u.getUsername());
        return u.getId();
    }

    // ==================== 商家注册 ====================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long registerMerchant(MerchantRegisterDTO dto) {
        captchaUtil.verify(dto.getCaptchaKey(), dto.getCaptchaCode());
        assertUsernameFree(dto.getUsername());

        User u = new User();
        u.setUsername(dto.getUsername());
        u.setPassword(BCrypt.hashpw(dto.getPassword()));
        u.setRealName(dto.getRealName());
        u.setPhone(dto.getPhone());
        u.setEmail(dto.getEmail());
        u.setCity(dto.getCity());
        u.setGender(dto.getGender());
        u.setBankAccount(dto.getBankAccount());
        u.setShopName(dto.getShopName());
        u.setBusinessLicense(dto.getBusinessLicense());
        u.setIdCardFront(dto.getIdCardFront());
        u.setIdCardBack(dto.getIdCardBack());
        u.setRole(UserRole.MERCHANT);
        u.setStatus(UserStatus.PENDING);
        u.setMerchantLevel(1);                // 新商家默认 1 级（费率 0.1%）
        u.setBalance(BigDecimal.ZERO);
        u.setPoints(0);
        userMapper.insert(u);

        log.info("新商家注册待审核 id={} username={} shopName={}", u.getId(), u.getUsername(), u.getShopName());
        return u.getId();
    }

    // ==================== 登录 ====================
    @Override
    public LoginVO login(LoginDTO dto) {
        // 1. 根据用户名查（LambdaQueryWrapper 利于避免硬编码字段名导致的笔误）
        User u = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        if (u == null) {
            throw BusinessException.of(ResultCode.USERNAME_OR_PASSWORD_WRONG);
        }

        // 2. BCrypt 密码校验（注意：BCrypt.checkpw 内部做了恒定时比较，能抵御时序攻击）
        if (!BCrypt.checkpw(dto.getPassword(), u.getPassword())) {
            throw BusinessException.of(ResultCode.USERNAME_OR_PASSWORD_WRONG);
        }

        // 3. 审核状态校验
        switch (u.getStatus()) {
            case PENDING:
                throw BusinessException.of(ResultCode.ACCOUNT_PENDING_AUDIT);
            case REJECTED:
                throw BusinessException.of(ResultCode.ACCOUNT_REJECTED,
                        "审核未通过：" + (u.getRejectReason() == null ? "请联系管理员" : u.getRejectReason()));
            case BANNED:
                // 封禁时限内直接拒绝；若 ban_until 已过期，则理论上应由定时任务自动解封，
                // 这里兜底：若已过期但状态还没刷新，放行
                if (u.getBanUntil() != null && u.getBanUntil().isBefore(LocalDateTime.now())) {
                    log.info("检测到封禁已过期，放行登录 id={}", u.getId());
                } else {
                    throw BusinessException.of(ResultCode.ACCOUNT_BANNED,
                            "您的账号已被封禁" + (u.getBanUntil() != null ? "至 " + u.getBanUntil() : ""));
                }
                break;
            case APPROVED:
            default:
                // 通过
        }

        // 4. 签发 JWT
        String token = jwtUtil.generate(u.getId(), u.getUsername(), u.getRole());
        log.info("登录成功 id={} username={} role={}", u.getId(), u.getUsername(), u.getRole());

        return LoginVO.builder()
                .token(token)
                .tokenPrefix(tokenPrefix)
                .user(UserVO.from(u))
                .build();
    }

    // ==================== 私有工具 ====================
    private void assertUsernameFree(String username) {
        Long exist = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (exist != null && exist > 0) {
            throw BusinessException.of(ResultCode.USERNAME_EXIST);
        }
    }
}
