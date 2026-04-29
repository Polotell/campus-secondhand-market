package com.campus.market.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 平台中间账户（ESCROW 托管货款、FEE 手续费归集）。
 * <p>与 {@code platform_account} 表对应，初始化两行见 {@code data.sql}。</p>
 */
@Data
@TableName("platform_account")
public class PlatformAccount implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final long ESCROW_ID = 1L;
    public static final long FEE_ID     = 2L;

    @TableId
    private Long id;
    /** ESCROW | FEE */
    private String type;
    private BigDecimal balance;
    private LocalDateTime updatedAt;
}
