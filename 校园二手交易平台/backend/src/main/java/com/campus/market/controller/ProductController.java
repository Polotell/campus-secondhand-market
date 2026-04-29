package com.campus.market.controller;

import com.campus.market.common.PageResult;
import com.campus.market.common.Result;
import com.campus.market.common.UserContext;
import com.campus.market.common.enums.UserRole;
import com.campus.market.service.ProductService;
import com.campus.market.service.ReviewService;
import com.campus.market.vo.ProductDetailVO;
import com.campus.market.vo.ProductListVO;
import com.campus.market.vo.ProductReviewVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 公开商品接口（所有人可访问，包括未登录的游客）
 * <ul>
 *   <li>GET /products          列表（仅 ON_SALE）</li>
 *   <li>GET /products/{id}     详情</li>
 * </ul>
 */
@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final ReviewService  reviewService;

    @GetMapping
    public Result<PageResult<ProductListVO>> list(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "latest") String sort,
            @RequestParam(required = false, defaultValue = "1")  long pageNum,
            @RequestParam(required = false, defaultValue = "20") long pageSize) {
        return Result.success(productService.listPublic(categoryId, keyword, sort, pageNum, pageSize));
    }

    /** 商品评价列表（公开） */
    @GetMapping("/{id}/reviews")
    public Result<PageResult<ProductReviewVO>> reviews(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1")  long pageNum,
            @RequestParam(defaultValue = "10") long pageSize) {
        return Result.success(reviewService.listProductReviews(id, pageNum, pageSize));
    }

    @GetMapping("/{id}")
    public Result<ProductDetailVO> detail(@PathVariable Long id) {
        Long viewerId = UserContext.getUserId();
        UserRole role = UserContext.getRole();
        return Result.success(productService.detail(id, viewerId, role == null ? null : role.name()));
    }
}
