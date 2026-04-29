package com.campus.market.dto;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * 结算下单请求体
 * <p>支持积分抵扣（100 积分 = 1 元，必须为 100 的整数倍）、线下约定时间地点。</p>
 */
@Data
public class CheckoutDTO {

    /**
     * 参与结算的购物车条目 ID 列表（必须属于当前用户且都是同一家商家）。
     * 前端只传勾选的；后端会再二次校验。
     */
    @NotEmpty(message = "请至少选择一件商品结算")
    @Size(max = 50, message = "一次下单最多 50 条")
    private List<Long> cartItemIds;

    /** 使用积分数量，100 积分 = 1 元，须为 100 的整数倍 */
    @Min(value = 0, message = "积分不能为负")
    private Integer pointsUsed = 0;

    private String remark;

    @Size(max = 200, message = "约定地点过长")
    private String meetPlace;

    @Size(max = 100, message = "约定时间过长")
    private String meetTime;
}
