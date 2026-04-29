package com.campus.market.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;

/** 商家对买家交易行为的评价 */
@Data
public class BuyerBehaviorReviewDTO {
    @Min(1) @Max(5)
    private int rating;
    @Size(max = 500)
    private String content;
}
