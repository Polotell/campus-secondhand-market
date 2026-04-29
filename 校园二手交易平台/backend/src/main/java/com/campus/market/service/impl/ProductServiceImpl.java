package com.campus.market.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.market.common.PageResult;
import com.campus.market.common.ResultCode;
import com.campus.market.common.enums.ProductStatus;
import com.campus.market.common.enums.UserRole;
import com.campus.market.dto.ProductCreateDTO;
import com.campus.market.entity.Product;
import com.campus.market.entity.ProductImage;
import com.campus.market.entity.User;
import com.campus.market.exception.BusinessException;
import com.campus.market.mapper.ProductImageMapper;
import com.campus.market.mapper.ProductMapper;
import com.campus.market.mapper.UserMapper;
import com.campus.market.service.CategoryService;
import com.campus.market.service.ProductService;
import com.campus.market.vo.ProductDetailVO;
import com.campus.market.vo.ProductListVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 商品服务实现
 * <p>
 * 关键要点：
 * <ul>
 *   <li><b>发布商品（create）</b>：同时写 product 主表 + product_image 图片表（多行），
 *       必须有 {@code @Transactional}；否则主表写成功、图片写失败会留下"无图商品"脏数据。</li>
 *   <li><b>列表回填</b>：列表返回时需要 shopName / categoryName / mainImage，为避免 N+1 查询，
 *       一次性根据 id 聚合 merchantId 集合 / categoryId 集合 / productId 集合，再合并。</li>
 *   <li><b>状态机</b>：商家只能对 PENDING/REJECTED/OFF_SHELF 做操作；LOCKED 和 SOLD 禁止改动（有订单锁定）。</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductMapper      productMapper;
    private final ProductImageMapper productImageMapper;
    private final UserMapper         userMapper;
    private final CategoryService    categoryService;

    // ==================== 列表 ====================

    @Override
    public PageResult<ProductListVO> listPublic(Long categoryId, String keyword,
                                                String sort,
                                                long pageNum, long pageSize) {
        if (pageNum  < 1) pageNum  = 1;
        if (pageSize < 1 || pageSize > 60) pageSize = 20;

        LambdaQueryWrapper<Product> q = new LambdaQueryWrapper<>();
        q.eq(Product::getStatus, ProductStatus.ON_SALE);
        if (categoryId != null) q.eq(Product::getCategoryId, categoryId);
        if (StrUtil.isNotBlank(keyword)) {
            q.and(w -> w.like(Product::getName, keyword)
                       .or().like(Product::getDescription, keyword));
        }
        applySort(q, sort);

        return buildPage(productMapper.selectPage(Page.of(pageNum, pageSize), q),
                pageNum, pageSize);
    }

    @Override
    public PageResult<ProductListVO> listByMerchant(Long merchantId, ProductStatus status,
                                                    long pageNum, long pageSize) {
        if (pageNum  < 1) pageNum  = 1;
        if (pageSize < 1 || pageSize > 60) pageSize = 20;
        LambdaQueryWrapper<Product> q = new LambdaQueryWrapper<>();
        q.eq(Product::getMerchantId, merchantId);
        if (status != null) q.eq(Product::getStatus, status);
        q.orderByDesc(Product::getCreatedAt);
        return buildPage(productMapper.selectPage(Page.of(pageNum, pageSize), q),
                pageNum, pageSize);
    }

    @Override
    public PageResult<ProductListVO> listForAdmin(ProductStatus status, String keyword,
                                                  long pageNum, long pageSize) {
        if (pageNum  < 1) pageNum  = 1;
        if (pageSize < 1 || pageSize > 60) pageSize = 20;
        LambdaQueryWrapper<Product> q = new LambdaQueryWrapper<>();
        if (status != null) q.eq(Product::getStatus, status);
        if (StrUtil.isNotBlank(keyword)) {
            q.like(Product::getName, keyword);
        }
        q.orderByDesc(Product::getCreatedAt);
        return buildPage(productMapper.selectPage(Page.of(pageNum, pageSize), q),
                pageNum, pageSize);
    }

    private void applySort(LambdaQueryWrapper<Product> q, String sort) {
        if (sort == null) sort = "latest";
        switch (sort) {
            case "price_asc":   q.orderByAsc(Product::getDiscountPrice); break;
            case "price_desc":  q.orderByDesc(Product::getDiscountPrice); break;
            case "sales_desc":  q.orderByDesc(Product::getSalesCount); break;
            case "rating_desc":
                q.orderByDesc(Product::getAvgRating)
                 .orderByDesc(Product::getGoodRate)
                 .orderByDesc(Product::getSalesCount);
                break;
            case "latest":
            default:            q.orderByDesc(Product::getCreatedAt); break;
        }
    }

    /** 把 IPage<Product> 转 PageResult<ProductListVO>，并一次性回填 shopName/categoryName/mainImage */
    private PageResult<ProductListVO> buildPage(Page<Product> page, long pageNum, long pageSize) {
        List<Product> rows = page.getRecords();
        if (rows.isEmpty()) {
            return PageResult.of(page, Collections.emptyList());
        }

        Set<Long> merchantIds = rows.stream().map(Product::getMerchantId).collect(Collectors.toSet());
        Set<Long> productIds  = rows.stream().map(Product::getId).collect(Collectors.toSet());

        Map<Long, String> merchantNameMap = userMapper.selectBatchIds(merchantIds).stream()
                .collect(Collectors.toMap(User::getId,
                        u -> StrUtil.blankToDefault(u.getShopName(), u.getUsername()),
                        (a, b) -> a));

        Map<Long, String> categoryNameMap = categoryService.idNameMap();

        // 批量查主图：每个 product 的 sort=0 的图
        Map<Long, String> mainImgMap = new HashMap<>();
        productImageMapper.selectList(new LambdaQueryWrapper<ProductImage>()
                        .in(ProductImage::getProductId, productIds)
                        .orderByAsc(ProductImage::getSort))
                .forEach(img -> mainImgMap.putIfAbsent(img.getProductId(), img.getUrl()));

        List<ProductListVO> list = rows.stream().map(p -> {
            ProductListVO v = ProductListVO.from(p);
            v.setShopName(merchantNameMap.get(p.getMerchantId()));
            v.setCategoryName(categoryNameMap.get(p.getCategoryId()));
            v.setMainImage(mainImgMap.get(p.getId()));
            return v;
        }).collect(Collectors.toList());

        return PageResult.of(page, list);
    }

    // ==================== 详情 ====================

    @Override
    public ProductDetailVO detail(Long id, Long viewerId, String viewerRole) {
        Product p = productMapper.selectById(id);
        if (p == null) throw BusinessException.of(ResultCode.PRODUCT_NOT_EXIST);

        // 状态可见性：商家自己 / ADMIN 任意可看；其他人只能看 ON_SALE/LOCKED/SOLD
        boolean isOwner = viewerId != null && viewerId.equals(p.getMerchantId());
        boolean isAdmin = UserRole.ADMIN.name().equals(viewerRole);
        boolean publicVisible = p.getStatus() == ProductStatus.ON_SALE
                || p.getStatus() == ProductStatus.LOCKED
                || p.getStatus() == ProductStatus.SOLD;
        if (!publicVisible && !isOwner && !isAdmin) {
            throw BusinessException.of(ResultCode.PRODUCT_NOT_ON_SALE);
        }

        ProductDetailVO v = ProductDetailVO.from(p);

        // 图片
        List<String> imgs = productImageMapper.selectList(new LambdaQueryWrapper<ProductImage>()
                        .eq(ProductImage::getProductId, id)
                        .orderByAsc(ProductImage::getSort))
                .stream().map(ProductImage::getUrl).collect(Collectors.toList());
        v.setImages(imgs);

        // 商家 & 分类
        User merchant = userMapper.selectById(p.getMerchantId());
        if (merchant != null) {
            v.setShopName(StrUtil.blankToDefault(merchant.getShopName(), merchant.getUsername()));
        }
        v.setCategoryName(categoryService.idNameMap().get(p.getCategoryId()));

        return v;
    }

    // ==================== 商家发布 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(Long merchantId, ProductCreateDTO dto) {
        if (dto.getDiscountPrice().compareTo(dto.getOriginalPrice()) > 0) {
            throw BusinessException.of(ResultCode.BAD_REQUEST, "折扣价不能大于原价");
        }

        Product p = new Product();
        p.setMerchantId(merchantId);
        p.setCategoryId(dto.getCategoryId());
        p.setName(dto.getName());
        p.setDescription(dto.getDescription());
        p.setOriginalPrice(dto.getOriginalPrice());
        p.setDiscountPrice(dto.getDiscountPrice());
        p.setSizeInfo(dto.getSizeInfo());
        p.setConditionLevel(dto.getConditionLevel());
        p.setStock(dto.getStock());
        p.setSalesCount(0);
        p.setNegotiable(dto.getNegotiable() == null ? 0 : dto.getNegotiable());
        p.setStatus(ProductStatus.PENDING);   // 提交审核
        productMapper.insert(p);

        // 写图片表，保证主图 sort=0
        List<String> imgs = dto.getImages();
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < imgs.size(); i++) {
            ProductImage img = ProductImage.builder()
                    .productId(p.getId())
                    .url(imgs.get(i))
                    .sort(i)
                    .createdAt(now)
                    .build();
            productImageMapper.insert(img);
        }

        log.info("商家 {} 发布商品 {} (id={})，进入 PENDING 审核队列", merchantId, p.getName(), p.getId());
        return p.getId();
    }

    // ==================== 上下架 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void offShelf(Long merchantId, Long productId) {
        Product p = loadOwnedProduct(merchantId, productId);
        if (p.getStatus() != ProductStatus.ON_SALE) {
            throw BusinessException.of(ResultCode.AUDIT_STATUS_ILLEGAL,
                    "当前状态 " + p.getStatus() + " 不能下架");
        }
        p.setStatus(ProductStatus.OFF_SHELF);
        productMapper.updateById(p);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onShelf(Long merchantId, Long productId) {
        Product p = loadOwnedProduct(merchantId, productId);
        if (p.getStatus() != ProductStatus.OFF_SHELF) {
            throw BusinessException.of(ResultCode.AUDIT_STATUS_ILLEGAL,
                    "当前状态 " + p.getStatus() + " 不能上架");
        }
        p.setStatus(ProductStatus.ON_SALE);
        productMapper.updateById(p);
    }

    private Product loadOwnedProduct(Long merchantId, Long productId) {
        Product p = productMapper.selectById(productId);
        if (p == null) throw BusinessException.of(ResultCode.PRODUCT_NOT_EXIST);
        if (!p.getMerchantId().equals(merchantId)) {
            throw BusinessException.of(ResultCode.FORBIDDEN, "不是你的商品");
        }
        return p;
    }

    // ==================== 管理员审核 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long productId) {
        Product p = productMapper.selectById(productId);
        if (p == null) throw BusinessException.of(ResultCode.PRODUCT_NOT_EXIST);
        if (p.getStatus() != ProductStatus.PENDING) {
            throw BusinessException.of(ResultCode.AUDIT_STATUS_ILLEGAL,
                    "当前状态 " + p.getStatus() + " 不允许审核通过");
        }
        p.setStatus(ProductStatus.ON_SALE);
        p.setRejectReason(null);
        productMapper.updateById(p);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long productId, String reason) {
        Product p = productMapper.selectById(productId);
        if (p == null) throw BusinessException.of(ResultCode.PRODUCT_NOT_EXIST);
        if (p.getStatus() != ProductStatus.PENDING) {
            throw BusinessException.of(ResultCode.AUDIT_STATUS_ILLEGAL,
                    "当前状态 " + p.getStatus() + " 不允许审核驳回");
        }
        p.setStatus(ProductStatus.REJECTED);
        p.setRejectReason(reason);
        productMapper.updateById(p);
    }
}
