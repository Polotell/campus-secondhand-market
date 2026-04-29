package com.campus.market.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.campus.market.common.enums.ConditionLevel;
import com.campus.market.common.enums.ProductStatus;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品实体（对应表 {@code product}）
 */
@Data
@TableName("product")
public class Product implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 商家用户 ID */
    private Long merchantId;

    /** 分类 ID */
    private Long categoryId;

    private String name;
    private String description;

    private BigDecimal originalPrice;
    /** 实际售价 */
    private BigDecimal discountPrice;

    private String sizeInfo;

    private ConditionLevel conditionLevel;

    private Integer stock;
    private Integer salesCount;

    /** 0 否 / 1 是 —— 是否允许议价 */
    private Integer negotiable;

    private BigDecimal goodRate;
    private BigDecimal avgRating;

    private ProductStatus status;
    private String        rejectReason;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
