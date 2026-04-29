package com.campus.market.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 购物车条目
 * <p>每个 {@code (user_id, product_id)} 唯一；数量/勾选状态可以累加或更新。</p>
 * <p>购物车不需要软删除——唯一索引 {@code uk_user_product(user_id, product_id, deleted)} 在软删下会产生冲突，
 * 这里直接物理删除（不加 @TableLogic），由 MP 的 deleteById/deleteBatchIds 执行真实 DELETE。</p>
 */
@Data
@TableName("cart_item")
public class CartItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;
    private Long productId;
    private Integer quantity;

    /** 1 勾选 / 0 取消勾选；结算时只对 selected=1 的生效 */
    private Integer selected;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    // 注意：购物车不做软删除。
    // 表上虽然保留了 deleted 列以及 uk_user_product(user_id, product_id, deleted) 唯一索引，
    // 但实体中故意不映射 deleted 字段：
    //   - 新行由 DB 默认值 0 写入；
    //   - 删除统一走物理 DELETE（MP 默认行为，因为没有 @TableLogic）；
    //   - 这样既绕过了"软删行 + 重新加购导致唯一索引冲突"的问题，
    //     也避免历史 deleted=1 行干扰 selectOne/selectList。
}
