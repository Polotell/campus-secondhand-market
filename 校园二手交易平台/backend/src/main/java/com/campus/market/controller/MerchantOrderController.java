package com.campus.market.controller;

import com.campus.market.annotation.RequiresRole;
import com.campus.market.common.PageResult;
import com.campus.market.common.Result;
import com.campus.market.common.UserContext;
import com.campus.market.common.enums.OrderStatus;
import com.campus.market.common.enums.UserRole;
import com.campus.market.dto.BuyerBehaviorReviewDTO;
import com.campus.market.dto.ReturnRejectDTO;
import com.campus.market.service.OrderService;
import com.campus.market.service.ReturnService;
import com.campus.market.service.ReviewService;
import com.campus.market.vo.OrderDetailVO;
import com.campus.market.vo.OrderListVO;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 商家订单接口。
 * <p>类级 {@link RequiresRole @RequiresRole(MERCHANT)} 保护，未通过商家审核的账号无法访问；
 * 服务层会再次校验"该订单的 merchantId == 当前用户 id"，避免越权查看他店订单。</p>
 */
@Validated
@RestController
@RequestMapping("/merchant/orders")
@RequiredArgsConstructor
@RequiresRole(UserRole.MERCHANT)
public class MerchantOrderController {

    private final OrderService  orderService;
    private final ReturnService returnService;
    private final ReviewService reviewService;

    /** 商家：我的店铺订单列表 */
    @GetMapping
    public Result<PageResult<OrderListVO>> list(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "1")  long pageNum,
            @RequestParam(defaultValue = "10") long pageSize) {
        return Result.success(orderService.listByMerchant(
                UserContext.getUserId(), status, pageNum, pageSize));
    }

    /** 商家：订单详情（仅本店铺可见） */
    @GetMapping("/{id}")
    public Result<OrderDetailVO> detail(@PathVariable Long id) {
        return Result.success(orderService.detailForMerchant(UserContext.getUserId(), id));
    }

    /** 商家：发货 PAID → SHIPPED */
    @PostMapping("/{id}/ship")
    public Result<Void> ship(@PathVariable Long id) {
        orderService.shipByMerchant(UserContext.getUserId(), id);
        return Result.success();
    }

    /** 商家：同意退货（核心事务：退款 + 还库存） */
    @PostMapping("/{id}/return-approve")
    public Result<Void> approveReturn(@PathVariable Long id) {
        returnService.approve(UserContext.getUserId(), id);
        return Result.success();
    }

    /** 商家：拒绝退货（订单回到 COMPLETED） */
    @PostMapping("/{id}/return-reject")
    public Result<Void> rejectReturn(@PathVariable Long id,
                                     @Valid @RequestBody ReturnRejectDTO dto) {
        returnService.reject(UserContext.getUserId(), id, dto);
        return Result.success();
    }

    /** 商家：对买家交易行为评价（影响买家好评率） */
    @PostMapping("/{id}/reviews/buyer")
    public Result<Void> reviewBuyer(@PathVariable Long id,
                                    @Valid @RequestBody BuyerBehaviorReviewDTO dto) {
        reviewService.submitBuyerReview(UserContext.getUserId(), id, dto);
        return Result.success();
    }
}
