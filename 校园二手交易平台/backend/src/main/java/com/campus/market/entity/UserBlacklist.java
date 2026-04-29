package com.campus.market.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 买家黑名单：{@code merchant_id = null} 表示全平台拉黑。
 */
@Data
@TableName("user_blacklist")
public class UserBlacklist implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;
    /** null = 平台级；非空 = 仅该商家店铺不可购买 */
    private Long merchantId;
    private String reason;
    private Long operatorId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableLogic
    private Integer deleted;
}
