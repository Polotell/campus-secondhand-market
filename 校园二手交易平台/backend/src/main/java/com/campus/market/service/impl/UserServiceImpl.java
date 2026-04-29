package com.campus.market.service.impl;

import com.campus.market.common.ResultCode;
import com.campus.market.common.UserContext;
import com.campus.market.entity.User;
import com.campus.market.exception.BusinessException;
import com.campus.market.mapper.UserMapper;
import com.campus.market.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    @Override
    public User getByIdOrThrow(Long id) {
        User u = userMapper.selectById(id);
        if (u == null) {
            throw BusinessException.of(ResultCode.NOT_FOUND, "用户不存在");
        }
        return u;
    }

    @Override
    public User getCurrentOrThrow() {
        Long uid = UserContext.getUserId();
        if (uid == null) {
            throw BusinessException.of(ResultCode.UNAUTHORIZED);
        }
        return getByIdOrThrow(uid);
    }
}
