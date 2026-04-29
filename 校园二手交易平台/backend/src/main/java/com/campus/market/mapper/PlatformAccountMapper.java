package com.campus.market.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.market.entity.PlatformAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface PlatformAccountMapper extends BaseMapper<PlatformAccount> {

    @Update("UPDATE platform_account SET balance = balance + #{amt}, updated_at = NOW() " +
            "WHERE id = #{id} AND type = #{type}")
    int addBalance(@Param("id") Long id, @Param("type") String type, @Param("amt") java.math.BigDecimal amt);

    @Update("UPDATE platform_account SET balance = balance - #{amt}, updated_at = NOW() " +
            "WHERE id = #{id} AND type = #{type} AND balance >= #{amt}")
    int deductBalance(@Param("id") Long id, @Param("type") String type, @Param("amt") java.math.BigDecimal amt);
}
