package com.campus.market.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.market.common.PageResult;
import com.campus.market.common.ResultCode;
import com.campus.market.common.enums.OrderStatus;
import com.campus.market.dto.BuyerBehaviorReviewDTO;
import com.campus.market.dto.MerchantServiceReviewDTO;
import com.campus.market.dto.SubmitProductReviewsDTO;
import com.campus.market.entity.*;
import com.campus.market.exception.BusinessException;
import com.campus.market.mapper.*;
import com.campus.market.service.ReviewService;
import com.campus.market.vo.ProductReviewVO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final OrderMapper         orderMapper;
    private final OrderItemMapper     orderItemMapper;
    private final ProductMapper       productMapper;
    private final ProductReviewMapper productReviewMapper;
    private final MerchantReviewMapper merchantReviewMapper;
    private final BuyerReviewMapper   buyerReviewMapper;
    private final UserMapper          userMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitProductReviews(Long buyerId, Long orderId, SubmitProductReviewsDTO dto) {
        Order o = mustOrderForBuyerReview(buyerId, orderId);
        for (SubmitProductReviewsDTO.Item it : dto.getItems()) {
            OrderItem oi = orderItemMapper.selectById(it.getOrderItemId());
            if (oi == null || !oi.getOrderId().equals(orderId)) {
                throw BusinessException.of(ResultCode.BAD_REQUEST, "订单明细不属于该订单");
            }
            if (oi.getReviewed() != null && oi.getReviewed() == 1) {
                throw BusinessException.of(ResultCode.REVIEW_DUPLICATE, "该商品已评价");
            }
            ProductReview pr = new ProductReview();
            pr.setOrderId(orderId);
            pr.setOrderItemId(it.getOrderItemId());
            pr.setProductId(oi.getProductId());
            pr.setBuyerId(buyerId);
            pr.setMerchantId(o.getMerchantId());
            pr.setRating(it.getRating());
            pr.setContent(it.getContent());
            pr.setImages(it.getImages());
            try {
                productReviewMapper.insert(pr);
            } catch (DuplicateKeyException e) {
                throw BusinessException.of(ResultCode.REVIEW_DUPLICATE);
            }
            oi.setReviewed(1);
            orderItemMapper.updateById(oi);
            refreshProductStats(oi.getProductId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitMerchantServiceReview(Long buyerId, Long orderId, MerchantServiceReviewDTO dto) {
        Order o = mustOrderForBuyerReview(buyerId, orderId);
        long cnt = merchantReviewMapper.selectCount(new LambdaQueryWrapper<MerchantReview>()
                .eq(MerchantReview::getOrderId, orderId));
        if (cnt > 0) throw BusinessException.of(ResultCode.REVIEW_DUPLICATE, "已评价过商家服务");
        MerchantReview mr = new MerchantReview();
        mr.setOrderId(orderId);
        mr.setBuyerId(buyerId);
        mr.setMerchantId(o.getMerchantId());
        mr.setRating(dto.getRating());
        mr.setContent(dto.getContent());
        try {
            merchantReviewMapper.insert(mr);
        } catch (DuplicateKeyException e) {
            throw BusinessException.of(ResultCode.REVIEW_DUPLICATE);
        }
        refreshMerchantGoodRate(o.getMerchantId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitBuyerReview(Long merchantId, Long orderId, BuyerBehaviorReviewDTO dto) {
        Order o = orderMapper.selectById(orderId);
        if (o == null) throw BusinessException.of(ResultCode.ORDER_NOT_EXIST);
        if (!o.getMerchantId().equals(merchantId)) throw BusinessException.of(ResultCode.FORBIDDEN);
        if (o.getStatus() != OrderStatus.COMPLETED) {
            throw BusinessException.of(ResultCode.REVIEW_NOT_ALLOWED, "订单未完成，不能评价买家");
        }
        long cnt = buyerReviewMapper.selectCount(new LambdaQueryWrapper<BuyerReview>()
                .eq(BuyerReview::getOrderId, orderId));
        if (cnt > 0) throw BusinessException.of(ResultCode.REVIEW_DUPLICATE, "已评价过该买家");
        BuyerReview br = new BuyerReview();
        br.setOrderId(orderId);
        br.setMerchantId(merchantId);
        br.setBuyerId(o.getBuyerId());
        br.setRating(dto.getRating());
        br.setContent(dto.getContent());
        try {
            buyerReviewMapper.insert(br);
        } catch (DuplicateKeyException e) {
            throw BusinessException.of(ResultCode.REVIEW_DUPLICATE);
        }
        refreshBuyerGoodRate(o.getBuyerId());
    }

    @Override
    public PageResult<ProductReviewVO> listProductReviews(Long productId, long pageNum, long pageSize) {
        if (pageNum < 1) pageNum = 1;
        if (pageSize < 1 || pageSize > 50) pageSize = 10;
        Page<ProductReview> page = productReviewMapper.selectPage(Page.of(pageNum, pageSize),
                new LambdaQueryWrapper<ProductReview>()
                        .eq(ProductReview::getProductId, productId)
                        .orderByDesc(ProductReview::getCreatedAt));
        if (page.getRecords().isEmpty()) return PageResult.of(page, List.of());
        Set<Long> uids = page.getRecords().stream().map(ProductReview::getBuyerId).collect(Collectors.toSet());
        Map<Long, User> uMap = userMapper.selectBatchIds(uids).stream()
                .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));
        List<ProductReviewVO> vos = page.getRecords().stream().map(r -> {
            ProductReviewVO v = new ProductReviewVO();
            v.setId(r.getId());
            v.setBuyerId(r.getBuyerId());
            User u = uMap.get(r.getBuyerId());
            v.setBuyerName(u == null ? "-" : StrUtil.blankToDefault(u.getRealName(), u.getUsername()));
            v.setRating(r.getRating());
            v.setContent(r.getContent());
            v.setImages(r.getImages());
            v.setCreatedAt(r.getCreatedAt());
            return v;
        }).collect(Collectors.toList());
        return PageResult.of(page, vos);
    }

    private Order mustOrderForBuyerReview(Long buyerId, Long orderId) {
        Order o = orderMapper.selectById(orderId);
        if (o == null) throw BusinessException.of(ResultCode.ORDER_NOT_EXIST);
        if (!o.getBuyerId().equals(buyerId)) throw BusinessException.of(ResultCode.FORBIDDEN);
        if (o.getStatus() != OrderStatus.COMPLETED) {
            throw BusinessException.of(ResultCode.REVIEW_NOT_ALLOWED, "订单未完成，不能评价");
        }
        return o;
    }

    private void refreshProductStats(Long productId) {
        List<ProductReview> reviews = productReviewMapper.selectList(
                new LambdaQueryWrapper<ProductReview>().eq(ProductReview::getProductId, productId));
        if (reviews.isEmpty()) return;
        double avg = reviews.stream().mapToInt(ProductReview::getRating).average().orElse(0);
        long good = reviews.stream().filter(r -> r.getRating() >= 4).count();
        Product p = productMapper.selectById(productId);
        if (p == null) return;
        p.setAvgRating(BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP));
        p.setGoodRate(BigDecimal.valueOf(good).divide(BigDecimal.valueOf(reviews.size()), 4, RoundingMode.HALF_UP));
        productMapper.updateById(p);
    }

    private void refreshMerchantGoodRate(Long merchantId) {
        List<MerchantReview> list = merchantReviewMapper.selectList(
                new LambdaQueryWrapper<MerchantReview>().eq(MerchantReview::getMerchantId, merchantId));
        User m = userMapper.selectById(merchantId);
        if (m == null) return;
        if (list.isEmpty()) {
            m.setGoodRate(BigDecimal.ONE);
            userMapper.updateById(m);
            return;
        }
        long good = list.stream().filter(r -> r.getRating() >= 4).count();
        m.setGoodRate(BigDecimal.valueOf(good).divide(BigDecimal.valueOf(list.size()), 4, RoundingMode.HALF_UP));
        userMapper.updateById(m);
    }

    private void refreshBuyerGoodRate(Long buyerId) {
        List<BuyerReview> list = buyerReviewMapper.selectList(
                new LambdaQueryWrapper<BuyerReview>().eq(BuyerReview::getBuyerId, buyerId));
        User b = userMapper.selectById(buyerId);
        if (b == null) return;
        if (list.isEmpty()) {
            b.setBuyerGoodRate(BigDecimal.ONE);
            userMapper.updateById(b);
            return;
        }
        long good = list.stream().filter(r -> r.getRating() >= 4).count();
        b.setBuyerGoodRate(BigDecimal.valueOf(good).divide(BigDecimal.valueOf(list.size()), 4, RoundingMode.HALF_UP));
        userMapper.updateById(b);
    }
}
