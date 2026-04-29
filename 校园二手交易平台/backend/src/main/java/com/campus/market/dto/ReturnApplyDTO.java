package com.campus.market.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 买家申请退货
 */
@Data
public class ReturnApplyDTO {

    @NotBlank(message = "退货原因不能为空")
    @Size(max = 500, message = "退货原因不能超过 500 字")
    private String reason;

    /** 凭证图（JSON 数组字符串），可空 */
    @Size(max = 4000, message = "图片字段过长")
    private String images;
}
