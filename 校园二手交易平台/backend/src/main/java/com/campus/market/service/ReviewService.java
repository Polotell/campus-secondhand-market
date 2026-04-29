package com.campus.market.service;

import com.campus.market.common.PageResult;
import com.campus.market.dto.BuyerBehaviorReviewDTO;
import com.campus.market.dto.MerchantServiceReviewDTO;
import com.campus.market.dto.SubmitProductReviewsDTO;
import com.campus.market.vo.ProductReviewVO;

/**
 * 评价：商品评价、商家服务态度（买家评）、买家交易行为（商家评）。
 * <p>对应实验报告必选「购买后评价 / 对商家服务态度评价 / 商家对买家评价影响好评率」。</p>
 */
public interface ReviewService {

    void submitProductReviews(Long buyerId, Long orderId, SubmitProductReviewsDTO dto);

    void submitMerchantServiceReview(Long buyerId, Long orderId, MerchantServiceReviewDTO dto);

    void submitBuyerReview(Long merchantId, Long orderId, BuyerBehaviorReviewDTO dto);

    PageResult<ProductReviewVO> listProductReviews(Long productId, long pageNum, long pageSize);
}
