package com.campus.market.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

/**
 * 更新购物车条目请求体
 * <p>数量与勾选状态均可选，只更新传了的字段。</p>
 */
@Data
public class CartUpdateDTO {

    @Min(value = 1, message = "数量至少 1")
    @Max(value = 999, message = "数量最多 999")
    private Integer quantity;

    /** 0/1 */
    private Integer selected;
}
