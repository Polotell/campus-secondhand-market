package com.campus.market.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 购物车视图（按商家分组 + 整体汇总）。
 */
@Data
public class CartViewVO implements Serializable {

    private int totalCount;          // 总条目数
    private int selectedCount;       // 勾选的条目数
    private BigDecimal selectedTotal = BigDecimal.ZERO; // 勾选的小计合计

    private List<ShopGroup> groups = new ArrayList<>();

    @Data
    public static class ShopGroup implements Serializable {
        private Long   merchantId;
        private String shopName;
        private List<CartItemVO> items = new ArrayList<>();
        private BigDecimal groupTotal = BigDecimal.ZERO;  // 本商家勾选商品总金额
    }
}
