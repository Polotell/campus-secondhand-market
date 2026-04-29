package com.campus.market.service;

import com.campus.market.dto.LoginDTO;
import com.campus.market.dto.MerchantRegisterDTO;
import com.campus.market.dto.UserRegisterDTO;
import com.campus.market.vo.LoginVO;

/**
 * 认证相关业务接口
 */
public interface AuthService {

    /** 普通用户注册，返回新用户 id（注册后需管理员审核通过才能登录） */
    Long registerUser(UserRegisterDTO dto);

    /** 商家注册，返回新用户 id */
    Long registerMerchant(MerchantRegisterDTO dto);

    /** 登录，返回 JWT + 用户信息 */
    LoginVO login(LoginDTO dto);
}
