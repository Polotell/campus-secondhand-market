package com.campus.market.dto;

import lombok.Data;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
public class AdminUserUpdateDTO {

    @Size(max = 50)
    private String realName;
    @Size(max = 20)
    private String phone;
    @Size(max = 80)
    private String email;
    @Size(max = 50)
    private String city;
    @Size(max = 10)
    private String gender;

    @Pattern(regexp = "^$|^\\d{16}$", message = "银行账号须为 16 位数字或留空")
    private String bankAccount;

    @Size(max = 100)
    private String shopName;
}
