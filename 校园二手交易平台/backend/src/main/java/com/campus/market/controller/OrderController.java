package com.campus.market.controller;

import com.campus.market.common.PageResult;
import com.campus.market.common.Result;
import com.campus.market.common.UserContext;
import com.campus.market.common.enums.OrderStatus;
import com.campus.market.dto.CheckoutDTO;
import com.campus.market.dto.MerchantServiceReviewDTO;
import com.campus.market.dto.ReturnApplyDTO;
import com.campus.market.dto.SubmitProductReviewsDTO;
import com.campus.market.service.OrderService;
import com.campus.market.service.ReturnService;
import com.campus.market.service.ReviewService;
import com.campus.market.vo.OrderDetailVO;
import com.campus.market.vo.OrderListVO;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * 买家订单接口
 */
@Validated
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService   orderService;
    private final ReturnService  returnService;
    private final ReviewService  reviewService;

    /** 结算预览：根据勾选的 cartItemIds 返回金额明细 */
    @PostMapping("/preview")
    public Result<OrderService.Preview> preview(@RequestBody Map<String, List<Long>> body) {
        List<Long> ids = body == null ? null : body.get("cartItemIds");
        return Result.success(orderService.preview(UserContext.getUserId(), ids));
    }

    /** 下单（核心事务） */
    @PostMapping
    public Result<Map<String, String>> create(@Valid @RequestBody CheckoutDTO dto) {
        Long id = orderService.create(UserContext.getUserId(), dto);
        return Result.success(Map.of("id", String.valueOf(id)));
    }

    /** 我的订单 */
    @GetMapping
    public Result<PageResult<OrderListVO>> list(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "1")  long pageNum,
            @RequestParam(defaultValue = "10") long pageSize) {
        return Result.success(orderService.listByBuyer(
                UserContext.getUserId(), status, pageNum, pageSize));
    }

    @GetMapping("/{id}")
    public Result<OrderDetailVO> detail(@PathVariable Long id) {
        return Result.success(orderService.detailForBuyer(UserContext.getUserId(), id));
    }

    @PostMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id) {
        orderService.cancelByBuyer(UserContext.getUserId(), id);
        return Result.success();
    }

    @PostMapping("/{id}/confirm-receive")
    public Result<Void> confirmReceive(@PathVariable Long id) {
        orderService.confirmReceive(UserContext.getUserId(), id);
        return Result.success();
    }

    /** 买家：申请退货（必须 RECEIVED 且未超 24h） */
    @PostMapping("/{id}/return-apply")
    public Result<Map<String, String>> applyReturn(@PathVariable Long id,
                                                 @Valid @RequestBody ReturnApplyDTO dto) {
        Long rid = returnService.apply(UserContext.getUserId(), id, dto);
        return Result.success(Map.of("returnRecordId", String.valueOf(rid)));
    }

    /** 买家：订单商品评价（文字 + 五星） */
    @PostMapping("/{id}/reviews/products")
    public Result<Void> reviewProducts(@PathVariable Long id,
                                       @Valid @RequestBody SubmitProductReviewsDTO dto) {
        reviewService.submitProductReviews(UserContext.getUserId(), id, dto);
        return Result.success();
    }

    /** 买家：商家服务态度评价 */
    @PostMapping("/{id}/reviews/merchant-service")
    public Result<Void> reviewMerchantService(@PathVariable Long id,
                                              @Valid @RequestBody MerchantServiceReviewDTO dto) {
        reviewService.submitMerchantServiceReview(UserContext.getUserId(), id, dto);
        return Result.success();
    }
}
