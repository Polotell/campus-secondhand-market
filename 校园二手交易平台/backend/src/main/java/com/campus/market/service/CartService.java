package com.campus.market.service;

import com.campus.market.vo.CartViewVO;

import java.util.List;

public interface CartService {

    /** 加入购物车；若已存在则累加数量（上限 999）。返回当前条目 id。 */
    Long add(Long userId, Long productId, Integer quantity);

    /** 查看购物车（按商家分组 + 勾选汇总） */
    CartViewVO view(Long userId);

    /** 改数量 / 勾选状态 */
    void update(Long userId, Long itemId, Integer quantity, Integer selected);

    /** 单条删除 */
    void remove(Long userId, Long itemId);

    /** 全选 / 全不选 */
    void selectAll(Long userId, boolean selected);

    /** 清空（仅删除勾选的） */
    void clearSelected(Long userId);
}
