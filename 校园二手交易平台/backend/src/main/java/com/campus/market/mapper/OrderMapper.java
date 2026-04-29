package com.campus.market.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.market.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 订单 Mapper
 * <p>关键自定义方法：</p>
 * <ul>
 *   <li>{@link #deductStock} 带乐观条件的库存扣减，返回 affectedRows。
 *       如果返回 0 说明库存被别的并发请求抢光，外层需回滚事务。</li>
 *   <li>{@link #deductBalance} 原子扣余额，返回 affectedRows。</li>
 * </ul>
 */
@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    /**
     * 原子扣减商品库存：仅当 {@code stock >= qty} 时才扣减并把 sales_count 加上去。
     * <p>用 SQL 层的 {@code stock >= #{qty}} 做乐观校验，就算多个线程同时并发，
     * 也只有满足条件的那一批能扣减成功，其余返回 0。</p>
     */
    @Update("UPDATE product " +
            "   SET stock = stock - #{qty}, " +
            "       sales_count = sales_count + #{qty}, " +
            "       updated_at = NOW() " +
            " WHERE id = #{productId} AND deleted = 0 AND status = 'ON_SALE' " +
            "   AND stock >= #{qty}")
    int deductStock(@Param("productId") Long productId, @Param("qty") Integer qty);

    /**
     * 原子扣减买家余额：仅当 {@code balance >= amount} 才扣减。
     */
    @Update("UPDATE user SET balance = balance - #{amount}, updated_at = NOW() " +
            " WHERE id = #{userId} AND deleted = 0 AND balance >= #{amount}")
    int deductBalance(@Param("userId") Long userId, @Param("amount") java.math.BigDecimal amount);

    /**
     * 退款：把钱还给买家（用于"买家取消"/"退货"/"下单事务内部回滚补偿"）
     */
    @Update("UPDATE user SET balance = balance + #{amount}, updated_at = NOW() " +
            " WHERE id = #{userId} AND deleted = 0")
    int refundBalance(@Param("userId") Long userId, @Param("amount") java.math.BigDecimal amount);

    /**
     * 归还商品库存（退货 / 下单失败回滚用）
     */
    @Update("UPDATE product " +
            "   SET stock = stock + #{qty}, " +
            "       sales_count = GREATEST(sales_count - #{qty}, 0), " +
            "       updated_at = NOW() " +
            " WHERE id = #{productId} AND deleted = 0")
    int restoreStock(@Param("productId") Long productId, @Param("qty") Integer qty);

    /** 仅读：按 id 锁当前用户最新余额（用于展示） */
    @Select("SELECT balance FROM user WHERE id = #{userId}")
    java.math.BigDecimal selectBalance(@Param("userId") Long userId);

    /** 商家已完成订单 GMV（用于动态等级） */
    @Select("SELECT COALESCE(SUM(actual_amount),0) FROM `order` " +
            "WHERE merchant_id = #{mid} AND status = 'COMPLETED' AND deleted = 0")
    java.math.BigDecimal sumCompletedGmvByMerchant(@Param("mid") Long mid);
}
