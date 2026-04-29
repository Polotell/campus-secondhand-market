package com.campus.market.service;

import com.campus.market.entity.User;

/**
 * 用户通用业务接口（查询、获取当前登录用户等）
 */
public interface UserService {

    /** 根据 id 获取用户，找不到抛 404 */
    User getByIdOrThrow(Long id);

    /** 获取当前登录用户（UserContext 里的 id 去数据库查最新） */
    User getCurrentOrThrow();
}
