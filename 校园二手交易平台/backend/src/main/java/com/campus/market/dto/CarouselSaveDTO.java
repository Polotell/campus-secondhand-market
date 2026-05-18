package com.campus.market.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class CarouselSaveDTO {

    @NotBlank(message = "图片地址不能为空")
    @Size(max = 2000)
    private String imageUrl;

    @Size(max = 2000)
    private String linkUrl;

    private Integer sort = 0;

    /** ON / OFF */
    @NotBlank
    @Size(max = 10)
    private String status = "ON";
}
