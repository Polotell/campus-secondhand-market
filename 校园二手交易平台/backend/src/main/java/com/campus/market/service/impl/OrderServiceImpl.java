package com.campus.market.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.market.common.PageResult;
import com.campus.market.common.ResultCode;
import com.campus.market.common.enums.OrderStatus;
import com.campus.market.common.enums.ProductStatus;
import com.campus.market.dto.CheckoutDTO;
import com.campus.market.entity.*;
import com.campus.market.exception.BusinessException;
import com.campus.market.mapper.*;
import com.campus.market.entity.ReturnRecord;
import com.campus.market.service.BlacklistService;
import com.campus.market.service.OrderService;
import com.campus.market.service.PlatformFinanceService;
import com.campus.market.service.PointsAccrualService;
import com.campus.market.service.ReturnService;
import com.campus.market.utils.MerchantFeeRate;
import com.campus.market.utils.OrderNoGenerator;
import com.campus.market.vo.CartItemVO;
import com.campus.market.vo.OrderDetailVO;
import com.campus.market.vo.OrderListVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 订单服务（核心模块）
 * <h3>下单事务核心设计</h3>
 * <p>
 * 下单涉及 <b>多表多行写入</b>，必须保证原子性：
 * <pre>
 *   扣商品库存       (product 表 N 行)
 *   扣买家余额       (user 表 1 行)
 *   写订单主表        (order 表 1 行)
 *   写订单明细        (order_item 表 N 行)
 *   清除购物车对应行  (cart_item 表 N 行)
 * </pre>
 *
 * <h4>事务原理（Spring 声明式事务 + AOP）：</h4>
 * <ol>
 *   <li>{@code @Transactional(rollbackFor = Exception.class)} 由 Spring AOP 织入；
 *       方法进入时开启事务、返回时提交；抛出 RuntimeException 或 checked Exception 都会回滚。</li>
 *   <li>这里所有 DAO 调用共享同一个数据库连接（ThreadLocal 绑定），
 *       回滚时未提交的 UPDATE/INSERT 会被丢弃，保证一致。</li>
 * </ol>
 *
 * <h4>并发安全设计：</h4>
 * <ol>
 *   <li><b>扣库存</b>：{@code UPDATE product SET stock=stock-? WHERE id=? AND stock &gt;= ?}。
 *       WHERE 条件里带乐观校验，多个并发只有库存够的才能成功，其余返回 0 行。
 *       返回 0 直接抛 {@link ResultCode#PRODUCT_STOCK_NOT_ENOUGH}，触发事务回滚。</li>
 *   <li><b>扣余额</b>：同样 {@code WHERE balance &gt;= ?}；并发下最多扣一次。</li>
 *   <li>无需额外加行锁（FOR UPDATE），避免锁等待 + 死锁风险。</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final CartItemMapper     cartItemMapper;
    private final OrderMapper        orderMapper;
    private final OrderItemMapper    orderItemMapper;
    private final ProductMapper      productMapper;
    private final ProductImageMapper productImageMapper;
    private final UserMapper             userMapper;
    private final ReturnService        returnService;
    private final PlatformFinanceService platformFinanceService;
    private final PointsAccrualService   pointsAccrualService;
    private final BlacklistService       blacklistService;

    /**
     * 自注入：用于在定时任务里走 Spring 代理调用子事务方法（{@code REQUIRES_NEW}）。
     * <p>直接 {@code this.doAutoConfirmOne()} 走的是裸对象，不会触发 AOP 事务织入；
     * 通过 {@code self} 调用才会经过代理对象，事务/AOP 才生效。
     * 用 {@code @Lazy} 是为了打破构造期循环依赖。</p>
     */
    @Autowired
    @Lazy
    private OrderServiceImpl self;

    // ============== 预览（只读） ==============

    @Override
    public Preview preview(Long buyerId, List<Long> cartItemIds) {
        List<CartItem> items = loadAndValidateCart(buyerId, cartItemIds);

        // 批量查商品，校验同一商家 & 在售 & 库存够 & 不买自己
        Set<Long> pids = items.stream().map(CartItem::getProductId).collect(Collectors.toSet());
        Map<Long, Product> pMap = productMapper.selectBatchIds(pids).stream()
                .collect(Collectors.toMap(Product::getId, p -> p, (a, b) -> a));
        Long merchantId = ensureSingleMerchant(items, pMap, buyerId);
        blacklistService.assertCanPurchase(buyerId, merchantId);

        // 汇总金额
        BigDecimal total = BigDecimal.ZERO;
        List<CartItemVO> voItems = new ArrayList<>();
        // 一次性查主图 & 商家
        Map<Long, String> imgMap = new HashMap<>();
        productImageMapper.selectList(new LambdaQueryWrapper<ProductImage>()
                        .in(ProductImage::getProductId, pids)
                        .orderByAsc(ProductImage::getSort))
                .forEach(img -> imgMap.putIfAbsent(img.getProductId(), img.getUrl()));

        User merchant = userMapper.selectById(merchantId);
        String shopName = merchant == null ? "-"
                : StrUtil.blankToDefault(merchant.getShopName(), merchant.getUsername());

        for (CartItem ci : items) {
            Product p = pMap.get(ci.getProductId());
            BigDecimal sub = p.getDiscountPrice().multiply(BigDecimal.valueOf(ci.getQuantity()));
            total = total.add(sub);

            CartItemVO v = new CartItemVO();
            v.setId(ci.getId());
            v.setProductId(p.getId());
            v.setProductName(p.getName());
            v.setProductImage(imgMap.get(p.getId()));
            v.setUnitPrice(p.getDiscountPrice());
            v.setQuantity(ci.getQuantity());
            v.setSelected(1);
            v.setSubtotal(sub);
            v.setStock(p.getStock());
            v.setProductStatus(p.getStatus());
            v.setMerchantId(merchantId);
            v.setShopName(shopName);
            v.setAvailable(true);
            voItems.add(v);
        }

        Preview pv = new Preview();
        pv.setMerchantId(merchantId);
        pv.setShopName(shopName);
        pv.setItems(voItems);
        BigDecimal totalScaled = total.setScale(2, RoundingMode.HALF_UP);
        pv.setTotalAmount(totalScaled);
        User buyerRow = userMapper.selectById(buyerId);
        int bp = buyerRow != null && buyerRow.getPoints() != null ? buyerRow.getPoints() : 0;
        BigDecimal maxDedYuan = new BigDecimal(bp).divide(BigDecimal.valueOf(100), 2, RoundingMode.DOWN);
        BigDecimal cap = totalScaled.min(maxDedYuan);
        int maxUsable = cap.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.DOWN).intValue();
        maxUsable = (maxUsable / 100) * 100;
        pv.setPointsUsable(maxUsable);
        pv.setActualAmount(totalScaled);

        BigDecimal bal = orderMapper.selectBalance(buyerId);
        pv.setBuyerBalance(bal == null ? BigDecimal.ZERO : bal);
        return pv;
    }

    // ============== 核心：下单 ==============

    /**
     * 核心下单：全程在单一事务里，任何一步失败都整体回滚。
     * <p>本方法是整个项目"事务 + 并发安全"的核心展示：参见类头 javadoc。</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public Long create(Long buyerId, CheckoutDTO dto) {
        // ---------- 1. 加载购物车 ----------
        List<CartItem> cartItems = loadAndValidateCart(buyerId, dto.getCartItemIds());

        // ---------- 2. 加载商品并校验 ----------
        Set<Long> pids = cartItems.stream().map(CartItem::getProductId).collect(Collectors.toSet());
        Map<Long, Product> pMap = productMapper.selectBatchIds(pids).stream()
                .collect(Collectors.toMap(Product::getId, p -> p, (a, b) -> a));
        Long merchantId = ensureSingleMerchant(cartItems, pMap, buyerId);
        blacklistService.assertCanPurchase(buyerId, merchantId);

        // ---------- 3. 计算订单金额 ----------
        BigDecimal total = BigDecimal.ZERO;
        for (CartItem ci : cartItems) {
            Product p = pMap.get(ci.getProductId());
            total = total.add(p.getDiscountPrice().multiply(BigDecimal.valueOf(ci.getQuantity())));
        }
        total = total.setScale(2, RoundingMode.HALF_UP);

        int pointsUsed = dto.getPointsUsed() == null ? 0 : dto.getPointsUsed();
        if (pointsUsed < 0 || pointsUsed % 100 != 0) {
            throw BusinessException.of(ResultCode.BAD_REQUEST, "使用积分须为 100 的正整数倍");
        }
        BigDecimal pointsDeduction = BigDecimal.valueOf(pointsUsed)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        if (pointsDeduction.compareTo(total) > 0) {
            throw BusinessException.of(ResultCode.BAD_REQUEST, "积分抵扣不能超过商品总金额");
        }
        if (pointsUsed > 0 && userMapper.deductPoints(buyerId, pointsUsed) == 0) {
            throw BusinessException.of(ResultCode.POINTS_NOT_ENOUGH);
        }
        BigDecimal actual = total.subtract(pointsDeduction).setScale(2, RoundingMode.HALF_UP);

        // ---------- 4. 计算费率（按实付 actual 快照） ----------
        User merchant = userMapper.selectById(merchantId);
        BigDecimal feeRate = MerchantFeeRate.rateOf(merchant == null ? null : merchant.getMerchantLevel());
        BigDecimal platformFee = actual.multiply(feeRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal merchantIncome = actual.subtract(platformFee);

        // ---------- 5. 扣库存（乐观 SQL，返回 0 = 被并发抢光） ----------
        // 注意：按 productId 升序扣减，避免多订单交叉导致死锁（先 A 后 B vs 先 B 后 A）
        List<CartItem> sorted = cartItems.stream()
                .sorted(Comparator.comparingLong(CartItem::getProductId))
                .collect(Collectors.toList());
        for (CartItem ci : sorted) {
            int ok = orderMapper.deductStock(ci.getProductId(), ci.getQuantity());
            if (ok == 0) {
                Product p = pMap.get(ci.getProductId());
                throw BusinessException.of(ResultCode.PRODUCT_STOCK_NOT_ENOUGH,
                        "商品「" + p.getName() + "」库存不足或已下架");
            }
        }

        // ---------- 6. 扣买家余额（实付为 0 则跳过） ----------
        if (actual.compareTo(BigDecimal.ZERO) > 0) {
            int ok = orderMapper.deductBalance(buyerId, actual);
            if (ok == 0) {
                throw BusinessException.of(ResultCode.BALANCE_NOT_ENOUGH);
            }
        }

        // ---------- 7. 写订单主表 ----------
        LocalDateTime now = LocalDateTime.now();
        Order order = new Order();
        order.setOrderNo(OrderNoGenerator.next());
        order.setBuyerId(buyerId);
        order.setMerchantId(merchantId);
        order.setTotalAmount(total);
        order.setPointsUsed(pointsUsed);
        order.setPointsDeduction(pointsDeduction);
        order.setActualAmount(actual);
        order.setPlatformFeeRate(feeRate);
        order.setPlatformFee(platformFee);
        order.setMerchantIncome(merchantIncome);
        order.setStatus(OrderStatus.PAID);
        order.setPaidAt(now);
        order.setRemark(dto.getRemark());
        order.setMeetPlace(dto.getMeetPlace());
        order.setMeetTime(dto.getMeetTime());
        orderMapper.insert(order);

        platformFinanceService.escrowIn(order);

        // ---------- 8. 写订单明细（含快照） ----------
        Map<Long, String> imgMap = new HashMap<>();
        productImageMapper.selectList(new LambdaQueryWrapper<ProductImage>()
                        .in(ProductImage::getProductId, pids)
                        .orderByAsc(ProductImage::getSort))
                .forEach(img -> imgMap.putIfAbsent(img.getProductId(), img.getUrl()));

        for (CartItem ci : cartItems) {
            Product p = pMap.get(ci.getProductId());
            BigDecimal sub = p.getDiscountPrice().multiply(BigDecimal.valueOf(ci.getQuantity()))
                    .setScale(2, RoundingMode.HALF_UP);
            OrderItem oi = OrderItem.builder()
                    .orderId(order.getId())
                    .productId(p.getId())
                    .productName(p.getName())
                    .productImage(imgMap.get(p.getId()))
                    .unitPrice(p.getDiscountPrice())
                    .quantity(ci.getQuantity())
                    .subtotal(sub)
                    .reviewed(0)
                    .createdAt(now)
                    .build();
            orderItemMapper.insert(oi);
        }

        // ---------- 9. 删除对应购物车条目 ----------
        cartItemMapper.deleteBatchIds(cartItems.stream().map(CartItem::getId).collect(Collectors.toList()));

        log.info("[下单成功] buyerId={} orderId={} orderNo={} amount={} merchantId={}",
                buyerId, order.getId(), order.getOrderNo(), actual, merchantId);
        return order.getId();
    }

    // ============== 查询 ==============

    @Override
    public PageResult<OrderListVO> listByBuyer(Long buyerId, OrderStatus status,
                                               long pageNum, long pageSize) {
        if (pageNum < 1) pageNum = 1;
        if (pageSize < 1 || pageSize > 60) pageSize = 10;
        LambdaQueryWrapper<Order> q = new LambdaQueryWrapper<Order>()
                .eq(Order::getBuyerId, buyerId)
                .orderByDesc(Order::getCreatedAt);
        if (status != null) q.eq(Order::getStatus, status);

        Page<Order> page = orderMapper.selectPage(Page.of(pageNum, pageSize), q);
        return assembleListPage(page);
    }

    /**
     * 把订单分页结果填充为 {@link OrderListVO}：聚合订单项 + 商家名 + 买家名，避免 N+1。
     */
    private PageResult<OrderListVO> assembleListPage(Page<Order> page) {
        if (page.getRecords().isEmpty()) return PageResult.of(page, Collections.emptyList());

        Set<Long> oids = page.getRecords().stream().map(Order::getId).collect(Collectors.toSet());
        Set<Long> mids = page.getRecords().stream().map(Order::getMerchantId).collect(Collectors.toSet());
        Set<Long> bids = page.getRecords().stream().map(Order::getBuyerId).collect(Collectors.toSet());

        List<OrderItem> allItems = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().in(OrderItem::getOrderId, oids)
                        .orderByAsc(OrderItem::getId));
        Map<Long, List<OrderItem>> itemMap = allItems.stream()
                .collect(Collectors.groupingBy(OrderItem::getOrderId));

        Set<Long> uids = new HashSet<>(); uids.addAll(mids); uids.addAll(bids);
        Map<Long, User> uMap = uids.isEmpty() ? Collections.emptyMap()
                : userMapper.selectBatchIds(uids).stream()
                    .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));

        List<OrderListVO> vos = page.getRecords().stream().map(o -> {
            OrderListVO v = new OrderListVO();
            v.setId(o.getId());
            v.setOrderNo(o.getOrderNo());
            v.setMerchantId(o.getMerchantId());
            User m = uMap.get(o.getMerchantId());
            v.setShopName(m == null ? "-" : StrUtil.blankToDefault(m.getShopName(), m.getUsername()));
            v.setBuyerId(o.getBuyerId());
            User b = uMap.get(o.getBuyerId());
            v.setBuyerName(b == null ? "-" : StrUtil.blankToDefault(b.getRealName(), b.getUsername()));
            v.setTotalAmount(o.getTotalAmount());
            v.setActualAmount(o.getActualAmount());
            v.setStatus(o.getStatus());
            v.setCreatedAt(o.getCreatedAt());
            v.setPaidAt(o.getPaidAt());

            List<OrderItem> is = itemMap.getOrDefault(o.getId(), Collections.emptyList());
            v.setItemCount(is.stream().mapToInt(OrderItem::getQuantity).sum());
            v.setItems(is.stream().limit(3).map(i -> {
                OrderListVO.OrderItemSnapshot s = new OrderListVO.OrderItemSnapshot();
                s.setProductId(i.getProductId());
                s.setProductName(i.getProductName());
                s.setProductImage(i.getProductImage());
                s.setUnitPrice(i.getUnitPrice());
                s.setQuantity(i.getQuantity());
                return s;
            }).collect(Collectors.toList()));
            return v;
        }).collect(Collectors.toList());
        return PageResult.of(page, vos);
    }

    @Override
    public OrderDetailVO detailForBuyer(Long buyerId, Long orderId) {
        Order o = orderMapper.selectById(orderId);
        if (o == null) throw BusinessException.of(ResultCode.ORDER_NOT_EXIST);
        if (!o.getBuyerId().equals(buyerId)) throw BusinessException.of(ResultCode.FORBIDDEN, "该订单不属于你");
        return assembleDetail(o);
    }

    private OrderDetailVO assembleDetail(Order o) {
        OrderDetailVO d = new OrderDetailVO();
        d.setId(o.getId()); d.setOrderNo(o.getOrderNo());
        d.setMerchantId(o.getMerchantId()); d.setBuyerId(o.getBuyerId());
        d.setTotalAmount(o.getTotalAmount()); d.setPointsUsed(o.getPointsUsed());
        d.setPointsDeduction(o.getPointsDeduction()); d.setActualAmount(o.getActualAmount());
        d.setPlatformFeeRate(o.getPlatformFeeRate()); d.setPlatformFee(o.getPlatformFee());
        d.setMerchantIncome(o.getMerchantIncome()); d.setStatus(o.getStatus());
        d.setPaidAt(o.getPaidAt()); d.setShippedAt(o.getShippedAt());
        d.setReceivedAt(o.getReceivedAt()); d.setAutoConfirmAt(o.getAutoConfirmAt());
        d.setReturnDeadline(o.getReturnDeadline()); d.setCompletedAt(o.getCompletedAt());
        d.setCancelledAt(o.getCancelledAt()); d.setCreatedAt(o.getCreatedAt());
        d.setRemark(o.getRemark());
        d.setMeetPlace(o.getMeetPlace());
        d.setMeetTime(o.getMeetTime());

        User m = userMapper.selectById(o.getMerchantId());
        d.setShopName(m == null ? "-" : StrUtil.blankToDefault(m.getShopName(), m.getUsername()));
        User b = userMapper.selectById(o.getBuyerId());
        d.setBuyerName(b == null ? "-" : StrUtil.blankToDefault(b.getRealName(), b.getUsername()));

        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, o.getId())
                        .orderByAsc(OrderItem::getId));
        d.setItems(items.stream().map(i -> {
            OrderDetailVO.OrderItemVO v = new OrderDetailVO.OrderItemVO();
            v.setId(i.getId()); v.setProductId(i.getProductId());
            v.setProductName(i.getProductName()); v.setProductImage(i.getProductImage());
            v.setUnitPrice(i.getUnitPrice()); v.setQuantity(i.getQuantity());
            v.setSubtotal(i.getSubtotal()); v.setReviewed(i.getReviewed());
            return v;
        }).collect(Collectors.toList()));

        // 退货记录（如有）+ 是否还能申请退货
        ReturnRecord rr = returnService.getByOrderId(o.getId());
        d.setReturnRecord(returnService.toVO(rr));
        d.setCanApplyReturn(rr == null
                && o.getStatus() == OrderStatus.RECEIVED
                && o.getReturnDeadline() != null
                && LocalDateTime.now().isBefore(o.getReturnDeadline()));
        return d;
    }

    // ============== 状态流转 ==============

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelByBuyer(Long buyerId, Long orderId) {
        Order o = orderMapper.selectById(orderId);
        if (o == null) throw BusinessException.of(ResultCode.ORDER_NOT_EXIST);
        if (!o.getBuyerId().equals(buyerId)) throw BusinessException.of(ResultCode.FORBIDDEN);
        if (o.getStatus() != OrderStatus.PAID) {
            throw BusinessException.of(ResultCode.ORDER_STATUS_ILLEGAL,
                    "订单当前状态 " + o.getStatus() + "，不能取消");
        }
        // 取消：归还库存 + 退款 + 改订单状态（一个事务内）
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId));
        for (OrderItem i : items) {
            orderMapper.restoreStock(i.getProductId(), i.getQuantity());
        }
        platformFinanceService.escrowOutToBuyerCancel(o);
        orderMapper.refundBalance(buyerId, o.getActualAmount());
        if (o.getPointsUsed() != null && o.getPointsUsed() > 0) {
            userMapper.addPoints(buyerId, o.getPointsUsed());
        }

        o.setStatus(OrderStatus.CANCELLED);
        o.setCancelledAt(LocalDateTime.now());
        orderMapper.updateById(o);
        log.info("[订单取消] orderId={} 退款 ¥{} 给 buyerId={}", orderId, o.getActualAmount(), buyerId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmReceive(Long buyerId, Long orderId) {
        Order o = orderMapper.selectById(orderId);
        if (o == null) throw BusinessException.of(ResultCode.ORDER_NOT_EXIST);
        if (!o.getBuyerId().equals(buyerId)) throw BusinessException.of(ResultCode.FORBIDDEN);
        if (o.getStatus() != OrderStatus.SHIPPED) {
            throw BusinessException.of(ResultCode.ORDER_STATUS_ILLEGAL,
                    "订单当前状态 " + o.getStatus() + "，不能确认收货");
        }
        LocalDateTime now = LocalDateTime.now();
        o.setStatus(OrderStatus.RECEIVED);
        o.setReceivedAt(now);
        o.setReturnDeadline(now.plusHours(24));
        orderMapper.updateById(o);
        log.info("[确认收货] orderId={} buyerId={}", orderId, buyerId);
    }

    // ============== 商家侧 ==============

    @Override
    public PageResult<OrderListVO> listByMerchant(Long merchantId, OrderStatus status,
                                                  long pageNum, long pageSize) {
        if (pageNum < 1) pageNum = 1;
        if (pageSize < 1 || pageSize > 60) pageSize = 10;
        LambdaQueryWrapper<Order> q = new LambdaQueryWrapper<Order>()
                .eq(Order::getMerchantId, merchantId)
                .orderByDesc(Order::getCreatedAt);
        if (status != null) q.eq(Order::getStatus, status);

        Page<Order> page = orderMapper.selectPage(Page.of(pageNum, pageSize), q);
        return assembleListPage(page);
    }

    @Override
    public OrderDetailVO detailForMerchant(Long merchantId, Long orderId) {
        Order o = orderMapper.selectById(orderId);
        if (o == null) throw BusinessException.of(ResultCode.ORDER_NOT_EXIST);
        if (!o.getMerchantId().equals(merchantId)) {
            throw BusinessException.of(ResultCode.FORBIDDEN, "该订单不属于你的店铺");
        }
        return assembleDetail(o);
    }

    /**
     * 商家发货（PAID → SHIPPED）。
     * <p>事务保证：状态切换 + 时间字段刷新原子完成。auto_confirm_at 为 shipped_at + 7 天，
     * 由定时任务 {@link #autoConfirmReceiveJob()} 在到期后自动推进到 RECEIVED。</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void shipByMerchant(Long merchantId, Long orderId) {
        Order o = orderMapper.selectById(orderId);
        if (o == null) throw BusinessException.of(ResultCode.ORDER_NOT_EXIST);
        if (!o.getMerchantId().equals(merchantId)) {
            throw BusinessException.of(ResultCode.FORBIDDEN, "该订单不属于你的店铺");
        }
        if (o.getStatus() != OrderStatus.PAID) {
            throw BusinessException.of(ResultCode.ORDER_STATUS_ILLEGAL,
                    "订单当前状态 " + o.getStatus() + "，不能发货");
        }
        LocalDateTime now = LocalDateTime.now();
        o.setStatus(OrderStatus.SHIPPED);
        o.setShippedAt(now);
        o.setAutoConfirmAt(now.plusDays(7));
        orderMapper.updateById(o);
        log.info("[商家发货] orderId={} merchantId={} autoConfirmAt={}", orderId, merchantId, o.getAutoConfirmAt());
    }

    // ============== 后台定时任务 ==============

    /**
     * 7 天自动确认收货：每分钟扫一次过期的 SHIPPED 订单。
     * <p>每个订单单独走子事务：单条失败不影响其它订单。
     * 使用 {@code auto_confirm_at <= NOW()} 走索引 idx_auto_confirm_at，扫表代价小。</p>
     */
    @Override
    @Scheduled(fixedDelay = 60_000L, initialDelay = 30_000L)
    public int autoConfirmReceiveJob() {
        LocalDateTime now = LocalDateTime.now();
        List<Order> due = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .eq(Order::getStatus, OrderStatus.SHIPPED)
                .isNotNull(Order::getAutoConfirmAt)
                .le(Order::getAutoConfirmAt, now)
                .last("LIMIT 200"));
        int handled = 0;
        for (Order o : due) {
            try {
                self.doAutoConfirmOne(o.getId());
                handled++;
            } catch (Exception ex) {
                log.warn("[自动确认收货] orderId={} 失败：{}", o.getId(), ex.getMessage());
            }
        }
        if (handled > 0) log.info("[自动确认收货] 本轮处理 {} 单", handled);
        return handled;
    }

    /**
     * 24h 退货窗口过期：每分钟扫一次需要完结的 RECEIVED 订单。
     */
    @Override
    @Scheduled(fixedDelay = 60_000L, initialDelay = 45_000L)
    public int autoCompleteJob() {
        LocalDateTime now = LocalDateTime.now();
        List<Order> due = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .eq(Order::getStatus, OrderStatus.RECEIVED)
                .isNotNull(Order::getReturnDeadline)
                .le(Order::getReturnDeadline, now)
                .last("LIMIT 200"));
        int handled = 0;
        for (Order o : due) {
            try {
                self.doAutoCompleteOne(o.getId());
                handled++;
            } catch (Exception ex) {
                log.warn("[自动完结] orderId={} 失败：{}", o.getId(), ex.getMessage());
            }
        }
        if (handled > 0) log.info("[自动完结] 本轮处理 {} 单", handled);
        return handled;
    }

    /**
     * 子事务：单条订单 SHIPPED → RECEIVED。
     * <p>用 {@code REQUIRES_NEW} 隔离失败影响；同时要求自身被 Spring 代理调用，
     * 因此从 {@link #autoConfirmReceiveJob()} 通过 {@code self} 注入或暴露 public 方法触发。</p>
     */
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public void doAutoConfirmOne(Long orderId) {
        Order o = orderMapper.selectById(orderId);
        if (o == null || o.getStatus() != OrderStatus.SHIPPED) return; // 状态在间隙被改动，跳过
        LocalDateTime now = LocalDateTime.now();
        o.setStatus(OrderStatus.RECEIVED);
        o.setReceivedAt(now);
        o.setReturnDeadline(now.plusHours(24));
        orderMapper.updateById(o);
    }

    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public void doAutoCompleteOne(Long orderId) {
        Order o = orderMapper.selectById(orderId);
        if (o == null || o.getStatus() != OrderStatus.RECEIVED) return;
        platformFinanceService.settleOrderCompleted(o);
        pointsAccrualService.rewardBuyerOnOrderCompleted(o);
        o.setStatus(OrderStatus.COMPLETED);
        o.setCompletedAt(LocalDateTime.now());
        orderMapper.updateById(o);
    }

    // ============== 公共辅助 ==============

    /** 加载并校验：每一条都必须属于当前用户；缺失即报错。 */
    private List<CartItem> loadAndValidateCart(Long buyerId, List<Long> ids) {
        if (ids == null || ids.isEmpty()) throw BusinessException.of(ResultCode.CART_EMPTY);
        List<CartItem> items = cartItemMapper.selectBatchIds(ids);
        if (items.size() != ids.size()) throw BusinessException.of(ResultCode.CART_ITEM_NOT_EXIST);
        for (CartItem ci : items) {
            if (!ci.getUserId().equals(buyerId)) {
                throw BusinessException.of(ResultCode.CART_ITEM_NOT_EXIST);
            }
        }
        return items;
    }

    /** 校验：所有商品必须都 ON_SALE、属于同一商家、买家不能买自己店铺商品；返回商家 id。 */
    private Long ensureSingleMerchant(List<CartItem> items, Map<Long, Product> pMap, Long buyerId) {
        Long mid = null;
        for (CartItem ci : items) {
            Product p = pMap.get(ci.getProductId());
            if (p == null) throw BusinessException.of(ResultCode.PRODUCT_NOT_EXIST);
            if (p.getStatus() != ProductStatus.ON_SALE) {
                throw BusinessException.of(ResultCode.PRODUCT_NOT_ON_SALE,
                        "商品「" + p.getName() + "」已下架");
            }
            if (p.getMerchantId().equals(buyerId)) {
                throw BusinessException.of(ResultCode.CANNOT_BUY_OWN_PRODUCT);
            }
            if (mid == null) mid = p.getMerchantId();
            else if (!mid.equals(p.getMerchantId())) {
                throw BusinessException.of(ResultCode.CART_MULTI_MERCHANT);
            }
        }
        return mid;
    }
}
