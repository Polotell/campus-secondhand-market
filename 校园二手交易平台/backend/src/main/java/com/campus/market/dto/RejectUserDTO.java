package com.campus.market.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 管理员驳回用户/商家注册申请
 */
@Data
public class RejectUserDTO {

    @NotBlank(message = "请填写驳回原因")
    @Size(min = 2, max = 200, message = "驳回原因长度 2~200 字")
    private String reason;
}
