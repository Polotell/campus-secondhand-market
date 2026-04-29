package com.campus.market.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 登录成功返回体
 */
@Data
@Builder
public class LoginVO {

    /** JWT 令牌，前端需要存入 localStorage/Pinia，并在后续请求加到 Authorization header */
    private String token;

    /** Token 前缀（Bearer ）方便前端拼接 */
    private String tokenPrefix;

    /** 登录用户基础信息 */
    private UserVO user;
}
