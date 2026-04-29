package com.campus.market.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * 加入购物车请求体
 */
@Data
public class CartAddDTO {

    @NotNull(message = "请选择商品")
    private Long productId;

    @NotNull
    @Min(value = 1, message = "数量至少 1")
    @Max(value = 999, message = "数量最多 999")
    private Integer quantity = 1;
}
