package com.campus.market.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("carousel")
public class Carousel implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String imageUrl;
    private String linkUrl;
    private Integer sort;
    /** ON / OFF */
    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
