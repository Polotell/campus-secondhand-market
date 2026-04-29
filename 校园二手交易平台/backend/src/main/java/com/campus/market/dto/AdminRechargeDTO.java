package com.campus.market.dto;

import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class AdminRechargeDTO {

    @NotNull(message = "金额不能为空")
    @DecimalMin(value = "0.01", message = "充值金额至少 0.01 元")
    private BigDecimal amount;
}
