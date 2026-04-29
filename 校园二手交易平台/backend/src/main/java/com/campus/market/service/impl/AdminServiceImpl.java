package com.campus.market.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.market.common.PageResult;
import com.campus.market.common.ResultCode;
import com.campus.market.common.UserContext;
import com.campus.market.common.enums.UserRole;
import com.campus.market.common.enums.UserStatus;
import com.campus.market.dto.AdminMerchantLevelDTO;
import com.campus.market.dto.AdminRechargeDTO;
import com.campus.market.dto.AdminUserUpdateDTO;
import com.campus.market.entity.OperationLog;
import com.campus.market.entity.User;
import com.campus.market.exception.BusinessException;
import com.campus.market.mapper.OperationLogMapper;
import com.campus.market.mapper.OrderMapper;
import com.campus.market.mapper.UserMapper;
import com.campus.market.service.AdminService;
import com.campus.market.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * 管理后台业务实现
 * <p>
 * 关键要点：
 * <ul>
 *   <li><b>Spring 事务：</b>{@code approveUser / rejectUser} 标注 {@code @Transactional(rollbackFor = Exception.class)}，
 *       保证"更新用户状态 + 写操作日志"二者要么同时成功，要么同时回滚；
 *       即使数据库/日志表瞬时故障也不会出现"状态已改但日志没留"的不可追溯情况。</li>
 *   <li><b>幂等 &amp; 状态机：</b>只有 PENDING 才能被 approve/reject；其它状态（已通过/已驳回/封禁）
 *       直接抛业务异常 10010。</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserMapper         userMapper;
    private final OperationLogMapper operationLogMapper;
    private final OrderMapper        orderMapper;

    @Override
    public PageResult<UserVO> listUsers(UserRole role, UserStatus status, String keyword,
                                        long pageNum, long pageSize) {
        if (pageNum  < 1) pageNum  = 1;
        if (pageSize < 1 || pageSize > 100) pageSize = 20;

        LambdaQueryWrapper<User> q = new LambdaQueryWrapper<>();
        // 管理后台列表不展示 ADMIN 自己
        q.ne(User::getRole, UserRole.ADMIN);
        if (role != null)   q.eq(User::getRole, role);
        if (status != null) q.eq(User::getStatus, status);
        if (StrUtil.isNotBlank(keyword)) {
            q.and(w -> w
                    .like(User::getUsername, keyword)
                    .or().like(User::getRealName, keyword)
                    .or().like(User::getPhone, keyword));
        }
        q.orderByDesc(User::getCreatedAt);

        Page<User> page = userMapper.selectPage(Page.of(pageNum, pageSize), q);

        return PageResult.of(page,
                page.getRecords().stream().map(UserVO::from).collect(Collectors.toList()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveUser(Long userId) {
        User u = loadUserForAudit(userId);
        u.setStatus(UserStatus.APPROVED);
        u.setRejectReason(null);
        userMapper.updateById(u);

        writeLog(u, "APPROVE", "审核通过用户/商家注册", "SUCCESS", null);
        log.info("管理员审核通过 uid={} username={} 操作员 adminId={}",
                u.getId(), u.getUsername(), UserContext.getUserId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectUser(Long userId, String reason) {
        User u = loadUserForAudit(userId);
        u.setStatus(UserStatus.REJECTED);
        u.setRejectReason(reason);
        userMapper.updateById(u);

        writeLog(u, "REJECT", "审核驳回：" + reason, "SUCCESS", null);
        log.info("管理员审核驳回 uid={} username={} 理由={} 操作员 adminId={}",
                u.getId(), u.getUsername(), reason, UserContext.getUserId());
    }

    @Override
    public UserVO getUser(Long userId) {
        User u = userMapper.selectById(userId);
        if (u == null) {
            throw BusinessException.of(ResultCode.NOT_FOUND, "用户不存在");
        }
        return UserVO.from(u);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUser(Long userId, AdminUserUpdateDTO dto) {
        User u = mustManageableUser(userId);
        if (StrUtil.isNotBlank(dto.getRealName())) u.setRealName(dto.getRealName());
        if (StrUtil.isNotBlank(dto.getPhone())) u.setPhone(dto.getPhone());
        if (StrUtil.isNotBlank(dto.getEmail())) u.setEmail(dto.getEmail());
        if (StrUtil.isNotBlank(dto.getCity())) u.setCity(dto.getCity());
        if (StrUtil.isNotBlank(dto.getGender())) u.setGender(dto.getGender());
        if (dto.getBankAccount() != null) u.setBankAccount(dto.getBankAccount());
        if (u.getRole() == UserRole.MERCHANT && StrUtil.isNotBlank(dto.getShopName())) {
            u.setShopName(dto.getShopName());
        }
        userMapper.updateById(u);
        writeLog(u, "UPDATE", "管理员修改用户资料", "SUCCESS", null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long userId) {
        User u = mustManageableUser(userId);
        userMapper.deleteById(userId);
        writeLog(u, "DELETE", "管理员删除用户（软删除）", "SUCCESS", null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rechargeUser(Long userId, AdminRechargeDTO dto) {
        User u = mustManageableUser(userId);
        int n = orderMapper.refundBalance(userId, dto.getAmount());
        if (n == 0) {
            throw BusinessException.of(ResultCode.INTERNAL_ERROR, "充值失败：用户记录异常");
        }
        writeLog(u, "RECHARGE", "管理员充值 ¥" + dto.getAmount(), "SUCCESS", null);
        log.info("管理员充值 uid={} +¥{} 操作员={}", userId, dto.getAmount(), UserContext.getUserId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setMerchantLevel(Long userId, AdminMerchantLevelDTO dto) {
        User u = userMapper.selectById(userId);
        if (u == null) {
            throw BusinessException.of(ResultCode.NOT_FOUND, "用户不存在");
        }
        if (u.getRole() != UserRole.MERCHANT) {
            throw BusinessException.of(ResultCode.BAD_REQUEST, "仅商家账号可设置等级");
        }
        u.setMerchantLevel(dto.getLevel());
        userMapper.updateById(u);
        writeLog(u, "LEVEL", "管理员设置商家等级为 " + dto.getLevel(), "SUCCESS", null);
    }

    // ==================== 私有工具 ====================

    private User mustManageableUser(Long userId) {
        User u = userMapper.selectById(userId);
        if (u == null) {
            throw BusinessException.of(ResultCode.NOT_FOUND, "用户不存在");
        }
        if (u.getRole() == UserRole.ADMIN) {
            throw BusinessException.of(ResultCode.FORBIDDEN, "不能对管理员账号执行此操作");
        }
        return u;
    }

    private User loadUserForAudit(Long userId) {
        User u = userMapper.selectById(userId);
        if (u == null) {
            throw BusinessException.of(ResultCode.NOT_FOUND, "用户不存在");
        }
        if (u.getStatus() != UserStatus.PENDING) {
            throw BusinessException.of(ResultCode.AUDIT_STATUS_ILLEGAL,
                    "当前状态为 " + u.getStatus() + "，无法再次审核");
        }
        if (u.getRole() == UserRole.ADMIN) {
            throw BusinessException.of(ResultCode.FORBIDDEN, "管理员账号无需审核");
        }
        return u;
    }

    private void writeLog(User target, String op, String desc, String status, String errMsg) {
        OperationLog logEntry = OperationLog.builder()
                .userId(UserContext.getUserId())
                .username(UserContext.getUsername())
                .module("ADMIN_AUDIT")
                .operation(desc + " | 目标 uid=" + target.getId() + " username=" + target.getUsername())
                .status(status)
                .errorMsg(errMsg)
                .createdAt(LocalDateTime.now())
                .build();
        operationLogMapper.insert(logEntry);
    }
}
