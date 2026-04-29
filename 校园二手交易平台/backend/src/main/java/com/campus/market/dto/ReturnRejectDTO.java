package com.campus.market.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 商家拒绝退货
 */
@Data
public class ReturnRejectDTO {

    @NotBlank(message = "拒绝理由不能为空")
    @Size(max = 500, message = "拒绝理由不能超过 500 字")
    private String remark;
}
