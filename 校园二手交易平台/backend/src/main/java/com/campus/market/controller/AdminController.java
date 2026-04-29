package com.campus.market.controller;

import com.campus.market.annotation.RequiresRole;
import com.campus.market.common.PageResult;
import com.campus.market.common.Result;
import com.campus.market.common.enums.ProductStatus;
import com.campus.market.common.enums.UserRole;
import com.campus.market.common.enums.UserStatus;
import com.campus.market.dto.AdminMerchantLevelDTO;
import com.campus.market.dto.AdminRechargeDTO;
import com.campus.market.dto.AdminUserUpdateDTO;
import com.campus.market.dto.BlacklistAddDTO;
import com.campus.market.dto.RejectUserDTO;
import com.campus.market.common.UserContext;
import com.campus.market.service.AdminService;
import com.campus.market.service.BlacklistService;
import com.campus.market.service.ProductService;
import com.campus.market.vo.UserBlacklistVO;
import com.campus.market.vo.ProductListVO;
import com.campus.market.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 管理后台接口（仅管理员可访问）
 * <p>
 * 通过 {@link RequiresRole @RequiresRole(UserRole.ADMIN)} 配合 {@code AuthAspect}
 * 统一拦截非 ADMIN 调用，返回 403；未登录则由拦截器返回 401。
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@RequiresRole(UserRole.ADMIN)
public class AdminController {

    private final AdminService     adminService;
    private final ProductService   productService;
    private final BlacklistService blacklistService;

    /**
     * 查询用户列表（默认只看 PENDING 的待审核用户）。
     *
     * @param role     可选：USER | MERCHANT
     * @param status   可选：PENDING | APPROVED | REJECTED | BANNED；默认 PENDING
     * @param keyword  可选：模糊匹配 用户名/真实姓名/手机号
     * @param pageNum  页码（从 1 开始，默认 1）
     * @param pageSize 每页数量（默认 20，最大 100）
     */
    @GetMapping("/users")
    public Result<PageResult<UserVO>> listUsers(
            @RequestParam(required = false) UserRole   role,
            @RequestParam(required = false, defaultValue = "PENDING") UserStatus status,
            @RequestParam(required = false) String     keyword,
            @RequestParam(required = false, defaultValue = "1")  long pageNum,
            @RequestParam(required = false, defaultValue = "20") long pageSize) {

        return Result.success(adminService.listUsers(role, status, keyword, pageNum, pageSize));
    }

    /** 审核通过 */
    @PostMapping("/users/{id}/approve")
    public Result<Void> approve(@PathVariable Long id) {
        adminService.approveUser(id);
        return Result.success();
    }

    /** 审核驳回（需传 reason） */
    @PostMapping("/users/{id}/reject")
    public Result<Void> reject(@PathVariable Long id, @Valid @RequestBody RejectUserDTO dto) {
        adminService.rejectUser(id, dto.getReason());
        return Result.success();
    }

    @GetMapping("/users/{id}")
    public Result<UserVO> getUser(@PathVariable Long id) {
        return Result.success(adminService.getUser(id));
    }

    @PutMapping("/users/{id}")
    public Result<Void> updateUser(@PathVariable Long id, @Valid @RequestBody AdminUserUpdateDTO dto) {
        adminService.updateUser(id, dto);
        return Result.success();
    }

    @DeleteMapping("/users/{id}")
    public Result<Void> deleteUser(@PathVariable Long id) {
        adminService.deleteUser(id);
        return Result.success();
    }

    @PostMapping("/users/{id}/recharge")
    public Result<Void> recharge(@PathVariable Long id, @Valid @RequestBody AdminRechargeDTO dto) {
        adminService.rechargeUser(id, dto);
        return Result.success();
    }

    @PostMapping("/users/{id}/merchant-level")
    public Result<Void> merchantLevel(@PathVariable Long id, @Valid @RequestBody AdminMerchantLevelDTO dto) {
        adminService.setMerchantLevel(id, dto);
        return Result.success();
    }

    // ==================== 全平台黑名单 ====================

    @GetMapping("/blacklist")
    public Result<PageResult<UserBlacklistVO>> listPlatformBlacklist(
            @RequestParam(defaultValue = "1")  long pageNum,
            @RequestParam(defaultValue = "20") long pageSize) {
        return Result.success(blacklistService.listPlatform(pageNum, pageSize));
    }

    @PostMapping("/blacklist")
    public Result<Void> addPlatformBlacklist(@Valid @RequestBody BlacklistAddDTO dto) {
        blacklistService.addByPlatform(UserContext.getUserId(), dto);
        return Result.success();
    }

    @DeleteMapping("/blacklist/{id}")
    public Result<Void> removePlatformBlacklist(@PathVariable Long id) {
        blacklistService.removeByPlatform(id);
        return Result.success();
    }

    // ==================== 商品审核 ====================

    /** 商品列表（管理员用）—— 可按状态 / 关键词查 */
    @GetMapping("/products")
    public Result<PageResult<ProductListVO>> listProducts(
            @RequestParam(required = false, defaultValue = "PENDING") ProductStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "1")  long pageNum,
            @RequestParam(required = false, defaultValue = "20") long pageSize) {
        return Result.success(productService.listForAdmin(status, keyword, pageNum, pageSize));
    }

    /** 商品审核通过（PENDING → ON_SALE） */
    @PostMapping("/products/{id}/approve")
    public Result<Void> approveProduct(@PathVariable Long id) {
        productService.approve(id);
        return Result.success();
    }

    /** 商品审核驳回（PENDING → REJECTED，需传原因） */
    @PostMapping("/products/{id}/reject")
    public Result<Void> rejectProduct(@PathVariable Long id,
                                      @Valid @RequestBody RejectUserDTO dto) {
        // 复用 RejectUserDTO 的 reason 校验逻辑
        productService.reject(id, dto.getReason());
        return Result.success();
    }
}
