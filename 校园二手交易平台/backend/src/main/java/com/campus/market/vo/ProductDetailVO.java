package com.campus.market.vo;

import com.campus.market.common.enums.ConditionLevel;
import com.campus.market.common.enums.ProductStatus;
import com.campus.market.entity.Product;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 商品详情（含商家信息 + 所有图片）
 */
@Data
public class ProductDetailVO implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long   id;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long   merchantId;
    private String shopName;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long   categoryId;
    private String categoryName;

    private String name;
    private String description;
    private BigDecimal originalPrice;
    private BigDecimal discountPrice;
    private String  sizeInfo;
    private ConditionLevel conditionLevel;
    private Integer stock;
    private Integer salesCount;
    private Integer negotiable;

    private BigDecimal goodRate;
    private BigDecimal avgRating;

    private ProductStatus status;
    private String        rejectReason;

    private List<String> images;

    private LocalDateTime createdAt;

    public static ProductDetailVO from(Product p) {
        ProductDetailVO v = new ProductDetailVO();
        v.setId(p.getId());
        v.setMerchantId(p.getMerchantId());
        v.setCategoryId(p.getCategoryId());
        v.setName(p.getName());
        v.setDescription(p.getDescription());
        v.setOriginalPrice(p.getOriginalPrice());
        v.setDiscountPrice(p.getDiscountPrice());
        v.setSizeInfo(p.getSizeInfo());
        v.setConditionLevel(p.getConditionLevel());
        v.setStock(p.getStock());
        v.setSalesCount(p.getSalesCount());
        v.setNegotiable(p.getNegotiable());
        v.setGoodRate(p.getGoodRate());
        v.setAvgRating(p.getAvgRating());
        v.setStatus(p.getStatus());
        v.setRejectReason(p.getRejectReason());
        v.setCreatedAt(p.getCreatedAt());
        return v;
    }
}
