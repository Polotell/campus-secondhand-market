package com.campus.market.service;

import com.campus.market.dto.ReturnApplyDTO;
import com.campus.market.dto.ReturnRejectDTO;
import com.campus.market.entity.ReturnRecord;
import com.campus.market.vo.ReturnRecordVO;

/**
 * 退货流程服务（v1）
 * <pre>
 * RECEIVED ──申请──► RETURN_APPLYING ──同意──► RETURN_APPROVED → RETURNED （退款 + 还库存）
 *                                  └──拒绝──► RETURN_REJECTED → COMPLETED （订单照常完结）
 * </pre>
 * <p>关键约束：</p>
 * <ul>
 *   <li>仅 {@code RECEIVED} 状态、且 {@code now &lt;= returnDeadline} 才能申请；</li>
 *   <li>同一订单同一时刻最多一条有效申请（DB 唯一索引 {@code uk_order} 兜底）；</li>
 *   <li>同意退货为核心事务：状态切换 + 退款 + 还库存 + 销量回滚 必须原子；</li>
 *   <li>拒绝退货不影响金额，只把订单推回 COMPLETED；</li>
 *   <li>所有操作都校验"当事人 vs 订单 owner"，避免越权。</li>
 * </ul>
 */
public interface ReturnService {

    /** 买家：发起退货申请 */
    Long apply(Long buyerId, Long orderId, ReturnApplyDTO dto);

    /** 商家：同意退货（执行退款 + 库存返还） */
    void approve(Long merchantId, Long orderId);

    /** 商家：拒绝退货（订单状态推回 COMPLETED） */
    void reject(Long merchantId, Long orderId, ReturnRejectDTO dto);

    /** 查询订单当前的退货记录（如有） */
    ReturnRecord getByOrderId(Long orderId);

    /** 把 entity 转成 VO */
    ReturnRecordVO toVO(ReturnRecord r);
}
