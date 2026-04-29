package com.campus.market.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class CarouselVO implements Serializable {
    private Long id;
    private String imageUrl;
    private String linkUrl;
    private Integer sort;
    private String status;
}
