package com.campus.market.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
public class BlacklistAddDTO {

    @NotNull(message = "被拉黑用户不能为空")
    private Long userId;

    @Size(max = 500)
    private String reason;
}
