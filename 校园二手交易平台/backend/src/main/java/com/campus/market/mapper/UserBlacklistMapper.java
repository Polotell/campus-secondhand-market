package com.campus.market.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.market.entity.UserBlacklist;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserBlacklistMapper extends BaseMapper<UserBlacklist> {

    @Select("SELECT COUNT(1) FROM user_blacklist WHERE deleted = 0 AND user_id = #{buyerId} " +
            "AND (merchant_id IS NULL OR merchant_id = #{merchantId})")
    int countBlockPurchase(@Param("buyerId") Long buyerId, @Param("merchantId") Long merchantId);
}
