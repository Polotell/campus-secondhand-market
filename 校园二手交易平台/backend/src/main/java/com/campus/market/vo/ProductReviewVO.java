package com.campus.market.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class ProductReviewVO implements Serializable {
    private Long id;
    private Long buyerId;
    private String buyerName;
    private Integer rating;
    private String content;
    private String images;
    private LocalDateTime createdAt;
}
