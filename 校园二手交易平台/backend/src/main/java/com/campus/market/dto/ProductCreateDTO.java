package com.campus.market.dto;

import com.campus.market.common.enums.ConditionLevel;
import lombok.Data;

import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * 商家发布商品请求体
 * <p>发布后默认进入 PENDING 状态，等待管理员审核。</p>
 */
@Data
public class ProductCreateDTO {

    @NotBlank(message = "请输入商品名称")
    @Size(max = 100, message = "商品名称最长 100 字")
    private String name;

    @NotNull(message = "请选择商品分类")
    private Long categoryId;

    @Size(max = 2000, message = "描述最长 2000 字")
    private String description;

    @NotNull(message = "请填写原价")
    @DecimalMin(value = "0.00", message = "价格必须 ≥ 0")
    @DecimalMax(value = "9999999.99", message = "价格过大")
    private BigDecimal originalPrice;

    @NotNull(message = "请填写售价")
    @DecimalMin(value = "0.01", message = "售价必须 > 0")
    @DecimalMax(value = "9999999.99", message = "售价过大")
    private BigDecimal discountPrice;

    @Size(max = 100)
    private String sizeInfo;

    @NotNull(message = "请选择新旧程度")
    private ConditionLevel conditionLevel;

    @NotNull(message = "请填写库存")
    @Min(value = 1, message = "库存至少 1")
    @Max(value = 9999, message = "库存最多 9999")
    private Integer stock;

    /** 是否允许议价 0/1，默认 0 */
    private Integer negotiable;

    /**
     * 图片地址列表：第 1 张自动作为主图。
     * 每个 URL 是 /file/upload 返回的相对路径，如 "/uploads/2026-04-22/xxx.jpg"。
     */
    @NotEmpty(message = "请至少上传 1 张商品图片")
    @Size(min = 1, max = 8, message = "图片数量 1~8 张")
    private List<String> images;
}
