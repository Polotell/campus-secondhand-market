package com.campus.market.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.market.common.ResultCode;
import com.campus.market.common.enums.OrderStatus;
import com.campus.market.common.enums.ReturnAuditStatus;
import com.campus.market.dto.ReturnApplyDTO;
import com.campus.market.dto.ReturnRejectDTO;
import com.campus.market.entity.Order;
import com.campus.market.entity.OrderItem;
import com.campus.market.entity.ReturnRecord;
import com.campus.market.exception.BusinessException;
import com.campus.market.mapper.OrderItemMapper;
import com.campus.market.mapper.OrderMapper;
import com.campus.market.mapper.ReturnRecordMapper;
import com.campus.market.mapper.UserMapper;
import com.campus.market.service.PlatformFinanceService;
import com.campus.market.service.PointsAccrualService;
import com.campus.market.service.ReturnService;
import com.campus.market.vo.ReturnRecordVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 退货流程实现。
 * <h3>事务原理</h3>
 * <p>同意退货的 {@link #approve(Long, Long)} 是核心事务：
 * 借助 Spring 声明式事务（{@code @Transactional}）+ AOP，
 * 把"退款给买家、回滚库存、回滚销量、订单/退货状态切换"纳入同一数据库事务，
 * 任一步失败整体回滚，避免出现"已退款但库存没回"或"库存回了但钱没退"的脏数据。</p>
 *
 * <h3>越权防护</h3>
 * <p>每一步都核对当事人身份：</p>
 * <ul>
 *   <li>{@link #apply(Long, Long, ReturnApplyDTO)} —— 买家必须是 order.buyerId</li>
 *   <li>{@link #approve(Long, Long)} / {@link #reject(Long, Long, ReturnRejectDTO)} —— 商家必须是 order.merchantId</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReturnServiceImpl implements ReturnService {

    private final ReturnRecordMapper     returnRecordMapper;
    private final OrderMapper            orderMapper;
    private final OrderItemMapper        orderItemMapper;
    private final PlatformFinanceService platformFinanceService;
    private final PointsAccrualService   pointsAccrualService;
    private final UserMapper             userMapper;

    // ============== 买家：申请退货 ==============

    /**
     * 买家发起退货申请：
     * <ol>
     *   <li>校验订单存在 + 属于当前买家；</li>
     *   <li>状态必须是 RECEIVED；</li>
     *   <li>当前时间 &le; return_deadline（24h 内）；</li>
     *   <li>当前没有有效退货记录（DB 唯一索引兜底，捕获 DuplicateKeyException 友好提示）；</li>
     *   <li>插入 PENDING 退货记录 + 订单状态切到 RETURN_APPLYING。</li>
     * </ol>
     * 这里不是一次扣款写入，但仍用 {@code @Transactional} 保护"插记录 + 改订单状态"两步原子。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long apply(Long buyerId, Long orderId, ReturnApplyDTO dto) {
        Order o = orderMapper.selectById(orderId);
        if (o == null) throw BusinessException.of(ResultCode.ORDER_NOT_EXIST);
        if (!o.getBuyerId().equals(buyerId)) throw BusinessException.of(ResultCode.FORBIDDEN);
        if (o.getStatus() != OrderStatus.RECEIVED) {
            throw BusinessException.of(ResultCode.ORDER_STATUS_ILLEGAL,
                    "订单当前状态 " + o.getStatus() + "，不能申请退货");
        }
        LocalDateTime now = LocalDateTime.now();
        if (o.getReturnDeadline() == null || now.isAfter(o.getReturnDeadline())) {
            throw BusinessException.of(ResultCode.RETURN_DEADLINE_EXCEEDED);
        }

        // 业务层先查一次，给出更友好的错误码；DB 唯一索引兜底防并发重复提交
        ReturnRecord exist = returnRecordMapper.selectOne(new LambdaQueryWrapper<ReturnRecord>()
                .eq(ReturnRecord::getOrderId, orderId)
                .last("LIMIT 1"));
        if (exist != null) {
            throw BusinessException.of(ResultCode.RETURN_RECORD_DUPLICATE);
        }

        ReturnRecord r = new ReturnRecord();
        r.setOrderId(orderId);
        r.setBuyerId(buyerId);
        r.setMerchantId(o.getMerchantId());
        r.setReason(dto.getReason());
        r.setImages(dto.getImages());
        r.setApplyTime(now);
        r.setAuditStatus(ReturnAuditStatus.PENDING);

        try {
            returnRecordMapper.insert(r);
        } catch (DuplicateKeyException ex) {
            // 并发场景下被唯一索引拦截
            throw BusinessException.of(ResultCode.RETURN_RECORD_DUPLICATE);
        }

        o.setStatus(OrderStatus.RETURN_APPLYING);
        orderMapper.updateById(o);

        log.info("[退货申请] orderId={} buyerId={} reason={}", orderId, buyerId, dto.getReason());
        return r.getId();
    }

    // ============== 商家：同意退货 ==============

    /**
     * 同意退货（核心事务）：
     * <pre>
     *   退还买家余额        (user.balance += actual_amount)
     *   回滚库存 + 销量     (product.stock += qty, sales_count -= qty 不小于 0)
     *   退货记录 → APPROVED
     *   订单 → RETURN_APPROVED → RETURNED （v1 简化为一步置 RETURNED）
     * </pre>
     * <p>使用 {@link OrderMapper#refundBalance} 与 {@link OrderMapper#restoreStock} 这两条
     * 已存在的乐观更新 SQL（带 deleted=0 条件），与下单时的扣减对称。</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long merchantId, Long orderId) {
        Order o = orderMapper.selectById(orderId);
        if (o == null) throw BusinessException.of(ResultCode.ORDER_NOT_EXIST);
        if (!o.getMerchantId().equals(merchantId)) {
            throw BusinessException.of(ResultCode.FORBIDDEN, "该订单不属于你的店铺");
        }
        if (o.getStatus() != OrderStatus.RETURN_APPLYING) {
            throw BusinessException.of(ResultCode.ORDER_STATUS_ILLEGAL,
                    "订单当前状态 " + o.getStatus() + "，不能同意退货");
        }
        ReturnRecord r = mustGetPending(orderId);

        // 1. 托管出账 + 退款给买家（货款原在 ESCROW）
        platformFinanceService.escrowOutToBuyerReturn(o);
        int rb = orderMapper.refundBalance(o.getBuyerId(), o.getActualAmount());
        if (rb == 0) {
            throw BusinessException.of(ResultCode.INTERNAL_ERROR, "退款失败：买家账号异常");
        }
        if (o.getPointsUsed() != null && o.getPointsUsed() > 0) {
            userMapper.addPoints(o.getBuyerId(), o.getPointsUsed());
        }

        // 2. 回滚库存 + 销量（按订单项逐条还）
        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, orderId));
        for (OrderItem it : items) {
            orderMapper.restoreStock(it.getProductId(), it.getQuantity());
        }

        // 3. 状态切换
        LocalDateTime now = LocalDateTime.now();
        r.setAuditStatus(ReturnAuditStatus.APPROVED);
        r.setAuditTime(now);
        returnRecordMapper.updateById(r);

        o.setStatus(OrderStatus.RETURNED);
        // v1 简化：审批成功直接落到 RETURNED，跳过 RETURN_APPROVED 中间态
        orderMapper.updateById(o);

        log.info("[退货同意] orderId={} merchantId={} 退款 ¥{} 给 buyerId={}",
                orderId, merchantId, o.getActualAmount(), o.getBuyerId());
    }

    // ============== 商家：拒绝退货 ==============

    /**
     * 拒绝退货：不动账，仅写状态与拒绝理由；订单回到 COMPLETED。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long merchantId, Long orderId, ReturnRejectDTO dto) {
        Order o = orderMapper.selectById(orderId);
        if (o == null) throw BusinessException.of(ResultCode.ORDER_NOT_EXIST);
        if (!o.getMerchantId().equals(merchantId)) {
            throw BusinessException.of(ResultCode.FORBIDDEN, "该订单不属于你的店铺");
        }
        if (o.getStatus() != OrderStatus.RETURN_APPLYING) {
            throw BusinessException.of(ResultCode.ORDER_STATUS_ILLEGAL,
                    "订单当前状态 " + o.getStatus() + "，不能拒绝退货");
        }
        ReturnRecord r = mustGetPending(orderId);

        LocalDateTime now = LocalDateTime.now();
        r.setAuditStatus(ReturnAuditStatus.REJECTED);
        r.setAuditRemark(dto.getRemark());
        r.setAuditTime(now);
        returnRecordMapper.updateById(r);

        // 拒绝退货 = 交易正常完结：托管拆给商家 + 手续费，并发积分
        platformFinanceService.settleOrderCompleted(o);
        pointsAccrualService.rewardBuyerOnOrderCompleted(o);

        o.setStatus(OrderStatus.COMPLETED);
        o.setCompletedAt(now);
        orderMapper.updateById(o);

        log.info("[退货拒绝] orderId={} merchantId={} 理由={}", orderId, merchantId, dto.getRemark());
    }

    // ============== 查询 ==============

    @Override
    public ReturnRecord getByOrderId(Long orderId) {
        if (orderId == null) return null;
        return returnRecordMapper.selectOne(new LambdaQueryWrapper<ReturnRecord>()
                .eq(ReturnRecord::getOrderId, orderId)
                .last("LIMIT 1"));
    }

    @Override
    public ReturnRecordVO toVO(ReturnRecord r) {
        if (r == null) return null;
        ReturnRecordVO v = new ReturnRecordVO();
        BeanUtil.copyProperties(r, v);
        return v;
    }

    // ============== 内部辅助 ==============

    private ReturnRecord mustGetPending(Long orderId) {
        ReturnRecord r = getByOrderId(orderId);
        if (r == null) throw BusinessException.of(ResultCode.RETURN_RECORD_NOT_EXIST);
        if (r.getAuditStatus() != ReturnAuditStatus.PENDING) {
            throw BusinessException.of(ResultCode.RETURN_STATUS_ILLEGAL,
                    "退货当前状态 " + r.getAuditStatus() + "，不能重复审核");
        }
        return r;
    }
}
