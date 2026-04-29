package com.campus.market.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Data
public class AdminMerchantLevelDTO {

    @NotNull
    @Min(1)
    @Max(5)
    private Integer level;
}
