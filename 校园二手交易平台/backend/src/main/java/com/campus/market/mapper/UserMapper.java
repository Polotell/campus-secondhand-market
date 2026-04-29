package com.campus.market.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.market.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 用户 Mapper（继承 {@link BaseMapper} 后自动具备 CRUD + 分页 + 条件构造器）
 * <p>简单查询直接用 QueryWrapper/LambdaQueryWrapper；
 * 复杂关联查询（如"带商家信息的订单列表"）另写 XML，放在 {@code resources/mapper/} 下。</p>
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Update("UPDATE `user` SET points = points - #{pts}, updated_at = NOW() " +
            "WHERE id = #{userId} AND deleted = 0 AND points >= #{pts}")
    int deductPoints(@Param("userId") Long userId, @Param("pts") int pts);

    @Update("UPDATE `user` SET points = points + #{pts}, updated_at = NOW() " +
            "WHERE id = #{userId} AND deleted = 0")
    int addPoints(@Param("userId") Long userId, @Param("pts") int pts);
}
