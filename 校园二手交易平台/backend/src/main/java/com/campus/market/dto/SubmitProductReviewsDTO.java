package com.campus.market.dto;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.*;
import java.util.List;

@Data
public class SubmitProductReviewsDTO {

    @NotEmpty(message = "请至少评价一件商品")
    @Valid
    private List<Item> items;

    @Data
    public static class Item {
        @NotNull(message = "订单明细不能为空")
        private Long orderItemId;
        @Min(1) @Max(5)
        private int rating;
        @Size(max = 1000)
        private String content;
        @Size(max = 4000)
        private String images;
    }
}
