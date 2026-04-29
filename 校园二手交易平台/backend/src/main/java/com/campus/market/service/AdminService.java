package com.campus.market.service;

import com.campus.market.common.PageResult;
import com.campus.market.common.enums.UserRole;
import com.campus.market.common.enums.UserStatus;
import com.campus.market.dto.AdminMerchantLevelDTO;
import com.campus.market.dto.AdminRechargeDTO;
import com.campus.market.dto.AdminUserUpdateDTO;
import com.campus.market.vo.UserVO;

/**
 * 管理后台业务接口（仅管理员可调用，权限由 {@code @RequiresRole(ADMIN)} 控制）
 */
public interface AdminService {

    /**
     * 查询用户/商家列表
     * @param role   USER | MERCHANT | null（不限）
     * @param status PENDING | APPROVED | REJECTED | BANNED | null（不限）
     * @param keyword 模糊匹配 用户名 / 真实姓名 / 手机号（可空）
     */
    PageResult<UserVO> listUsers(UserRole role, UserStatus status, String keyword,
                                 long pageNum, long pageSize);

    /** 审核通过 */
    void approveUser(Long userId);

    /** 审核驳回，需写驳回原因 */
    void rejectUser(Long userId, String reason);

    /** 查看用户详情 */
    UserVO getUser(Long userId);

    /** 修改用户资料（不含密码） */
    void updateUser(Long userId, AdminUserUpdateDTO dto);

    /** 软删除用户 */
    void deleteUser(Long userId);

    /** 钱包充值（实验报告必选） */
    void rechargeUser(Long userId, AdminRechargeDTO dto);

    /** 设置商家等级 1~5（与定时任务并存） */
    void setMerchantLevel(Long userId, AdminMerchantLevelDTO dto);
}
