package com.campus.market.vo;

import com.campus.market.common.enums.UserRole;
import com.campus.market.common.enums.UserStatus;
import com.campus.market.entity.User;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户视图对象（返给前端用）
 * <p>从 {@link User} 裁剪掉敏感字段（password/deleted）后对外暴露。</p>
 */
@Data
public class UserVO {

    private Long id;
    private String username;
    private String realName;
    private String phone;
    private String email;
    private String city;
    private String gender;
    private String bankAccount;
    private String avatar;

    private UserRole   role;
    private UserStatus status;
    private String     rejectReason;
    private LocalDateTime banUntil;

    private String shopName;
    private String businessLicense;
    private String idCardFront;
    private String idCardBack;
    private Integer merchantLevel;
    private BigDecimal goodRate;
    private BigDecimal buyerGoodRate;

    private BigDecimal balance;
    private Integer    points;

    private LocalDateTime createdAt;

    /** 把 Entity 转成 VO（屏蔽 password 等敏感字段） */
    public static UserVO from(User u) {
        if (u == null) return null;
        UserVO v = new UserVO();
        v.setId(u.getId());
        v.setUsername(u.getUsername());
        v.setRealName(u.getRealName());
        v.setPhone(u.getPhone());
        v.setEmail(u.getEmail());
        v.setCity(u.getCity());
        v.setGender(u.getGender());
        v.setBankAccount(u.getBankAccount());
        v.setAvatar(u.getAvatar());
        v.setRole(u.getRole());
        v.setStatus(u.getStatus());
        v.setRejectReason(u.getRejectReason());
        v.setBanUntil(u.getBanUntil());
        v.setShopName(u.getShopName());
        v.setBusinessLicense(u.getBusinessLicense());
        v.setIdCardFront(u.getIdCardFront());
        v.setIdCardBack(u.getIdCardBack());
        v.setMerchantLevel(u.getMerchantLevel());
        v.setGoodRate(u.getGoodRate());
        v.setBuyerGoodRate(u.getBuyerGoodRate());
        v.setBalance(u.getBalance());
        v.setPoints(u.getPoints());
        v.setCreatedAt(u.getCreatedAt());
        return v;
    }
}
