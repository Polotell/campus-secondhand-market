package com.campus.market.service;

import com.campus.market.common.PageResult;
import com.campus.market.common.enums.ProductStatus;
import com.campus.market.dto.ProductCreateDTO;
import com.campus.market.vo.ProductDetailVO;
import com.campus.market.vo.ProductListVO;

/**
 * 商品业务接口
 */
public interface ProductService {

    /** 公开列表：ON_SALE 状态，支持分类/关键词/价格区间/排序 */
    PageResult<ProductListVO> listPublic(Long categoryId, String keyword,
                                         String sort,
                                         long pageNum, long pageSize);

    /** 商家查看自己的商品（可按状态过滤，null 则全部） */
    PageResult<ProductListVO> listByMerchant(Long merchantId, ProductStatus status,
                                             long pageNum, long pageSize);

    /** 管理员：查任意状态 */
    PageResult<ProductListVO> listForAdmin(ProductStatus status, String keyword,
                                           long pageNum, long pageSize);

    /** 商品详情（任何人可看，但若为非 ON_SALE/LOCKED 状态仅商家自己和管理员可看） */
    ProductDetailVO detail(Long id, Long viewerId, String viewerRole);

    /**
     * 商家发布商品，一并写入图片表（事务保护）
     * @return 新商品 ID
     */
    Long create(Long merchantId, ProductCreateDTO dto);

    /** 商家下架商品（ON_SALE → OFF_SHELF） */
    void offShelf(Long merchantId, Long productId);

    /** 商家重新上架（OFF_SHELF → ON_SALE） */
    void onShelf(Long merchantId, Long productId);

    /** 管理员审核通过（PENDING → ON_SALE） */
    void approve(Long productId);

    /** 管理员驳回（PENDING → REJECTED） */
    void reject(Long productId, String reason);
}
