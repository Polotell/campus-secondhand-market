package com.campus.market.vo;

import com.campus.market.common.enums.ConditionLevel;
import com.campus.market.common.enums.ProductStatus;
import com.campus.market.entity.Product;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品列表项（瀑布流/网格卡片用）
 */
@Data
public class ProductListVO implements Serializable {

    private Long   id;
    private Long   merchantId;
    private String shopName;
    private Long   categoryId;
    private String categoryName;

    private String name;
    private BigDecimal originalPrice;
    private BigDecimal discountPrice;
    private ConditionLevel conditionLevel;
    private Integer stock;
    private Integer salesCount;
    private ProductStatus status;
    private String  rejectReason;

    /** 主图 URL（相对路径，前端拼 /api 前缀） */
    private String mainImage;

    private LocalDateTime createdAt;

    public static ProductListVO from(Product p) {
        if (p == null) return null;
        ProductListVO v = new ProductListVO();
        v.setId(p.getId());
        v.setMerchantId(p.getMerchantId());
        v.setCategoryId(p.getCategoryId());
        v.setName(p.getName());
        v.setOriginalPrice(p.getOriginalPrice());
        v.setDiscountPrice(p.getDiscountPrice());
        v.setConditionLevel(p.getConditionLevel());
        v.setStock(p.getStock());
        v.setSalesCount(p.getSalesCount());
        v.setStatus(p.getStatus());
        v.setRejectReason(p.getRejectReason());
        v.setCreatedAt(p.getCreatedAt());
        return v;
    }
}
