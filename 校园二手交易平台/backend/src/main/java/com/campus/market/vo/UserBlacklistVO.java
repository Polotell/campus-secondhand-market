package com.campus.market.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class UserBlacklistVO implements Serializable {
    private Long id;
    private Long userId;
    private String buyerName;
    private Long merchantId;
    private String shopName;
    private String reason;
    private Long operatorId;
    private LocalDateTime createdAt;
}
