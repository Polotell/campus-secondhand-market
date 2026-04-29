package com.campus.market.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.campus.market.common.ResultCode;
import com.campus.market.common.enums.ProductStatus;
import com.campus.market.entity.CartItem;
import com.campus.market.entity.Product;
import com.campus.market.entity.ProductImage;
import com.campus.market.entity.User;
import com.campus.market.exception.BusinessException;
import com.campus.market.mapper.CartItemMapper;
import com.campus.market.mapper.ProductImageMapper;
import com.campus.market.mapper.ProductMapper;
import com.campus.market.mapper.UserMapper;
import com.campus.market.service.BlacklistService;
import com.campus.market.service.CartService;
import com.campus.market.vo.CartItemVO;
import com.campus.market.vo.CartViewVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 购物车服务
 * <p>关键点：</p>
 * <ul>
 *   <li>{@code add()} 采用 UPSERT：已存在就累加数量，且由 {@code uk_user_product} 唯一索引兜底并发</li>
 *   <li>{@code view()} 一次查出所有商品/主图/商家，避免 N+1</li>
 *   <li>可用性字段 {@code available/unavailableReason}：下架 / 被删 / 库存不足时标记，不阻塞展示</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartItemMapper     cartItemMapper;
    private final ProductMapper      productMapper;
    private final ProductImageMapper productImageMapper;
    private final UserMapper         userMapper;
    private final BlacklistService   blacklistService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long add(Long userId, Long productId, Integer quantity) {
        if (quantity == null || quantity <= 0) quantity = 1;

        Product p = productMapper.selectById(productId);
        if (p == null) throw BusinessException.of(ResultCode.PRODUCT_NOT_EXIST);
        if (p.getStatus() != ProductStatus.ON_SALE) {
            throw BusinessException.of(ResultCode.PRODUCT_NOT_ON_SALE);
        }
        if (p.getMerchantId().equals(userId)) {
            throw BusinessException.of(ResultCode.CANNOT_BUY_OWN_PRODUCT);
        }
        blacklistService.assertCanPurchase(userId, p.getMerchantId());

        CartItem exist = cartItemMapper.selectOne(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId)
                .eq(CartItem::getProductId, productId));

        int finalQty;
        if (exist != null) {
            finalQty = Math.min(exist.getQuantity() + quantity, 999);
            exist.setQuantity(finalQty);
            exist.setSelected(1);  // 重新加入购物车默认勾选
            cartItemMapper.updateById(exist);
            return exist.getId();
        } else {
            finalQty = Math.min(quantity, 999);
            CartItem c = new CartItem();
            c.setUserId(userId);
            c.setProductId(productId);
            c.setQuantity(finalQty);
            c.setSelected(1);
            cartItemMapper.insert(c);
            return c.getId();
        }
    }

    @Override
    public CartViewVO view(Long userId) {
        List<CartItem> items = cartItemMapper.selectList(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId)
                .orderByDesc(CartItem::getCreatedAt));

        CartViewVO vo = new CartViewVO();
        if (items.isEmpty()) return vo;

        Set<Long> pids = items.stream().map(CartItem::getProductId).collect(Collectors.toSet());
        Map<Long, Product> pMap = productMapper.selectBatchIds(pids).stream()
                .collect(Collectors.toMap(Product::getId, p -> p, (a, b) -> a));

        Set<Long> mids = pMap.values().stream().map(Product::getMerchantId).collect(Collectors.toSet());
        Map<Long, User> mMap = mids.isEmpty() ? Collections.emptyMap()
                : userMapper.selectBatchIds(mids).stream()
                    .collect(Collectors.toMap(User::getId, m -> m, (a, b) -> a));

        // 查主图：每个 product 的 sort=0
        Map<Long, String> imgMap = new HashMap<>();
        if (!pids.isEmpty()) {
            productImageMapper.selectList(new LambdaQueryWrapper<ProductImage>()
                    .in(ProductImage::getProductId, pids)
                    .orderByAsc(ProductImage::getSort))
                    .forEach(img -> imgMap.putIfAbsent(img.getProductId(), img.getUrl()));
        }

        // 按商家分组
        Map<Long, CartViewVO.ShopGroup> groupMap = new LinkedHashMap<>();
        for (CartItem ci : items) {
            Product p = pMap.get(ci.getProductId());
            CartItemVO civ = new CartItemVO();
            civ.setId(ci.getId());
            civ.setProductId(ci.getProductId());
            civ.setQuantity(ci.getQuantity());
            civ.setSelected(ci.getSelected());
            civ.setCreatedAt(ci.getCreatedAt());

            if (p == null) {
                civ.setProductName("【商品已被删除】");
                civ.setUnitPrice(BigDecimal.ZERO);
                civ.setStock(0);
                civ.setAvailable(false);
                civ.setUnavailableReason("商品已被删除");
                civ.setSubtotal(BigDecimal.ZERO);
            } else {
                civ.setProductName(p.getName());
                civ.setProductImage(imgMap.get(p.getId()));
                civ.setUnitPrice(p.getDiscountPrice());
                civ.setStock(p.getStock());
                civ.setProductStatus(p.getStatus());
                civ.setMerchantId(p.getMerchantId());

                User m = mMap.get(p.getMerchantId());
                civ.setShopName(m == null ? "-"
                        : StrUtil.blankToDefault(m.getShopName(), m.getUsername()));

                boolean avail = p.getStatus() == ProductStatus.ON_SALE
                        && p.getStock() >= ci.getQuantity();
                civ.setAvailable(avail);
                if (!avail) {
                    civ.setUnavailableReason(p.getStatus() != ProductStatus.ON_SALE
                            ? "商品已下架" : "库存不足");
                }
                civ.setSubtotal(p.getDiscountPrice()
                        .multiply(BigDecimal.valueOf(ci.getQuantity())));
            }

            Long gKey = (p == null) ? 0L : p.getMerchantId();
            CartViewVO.ShopGroup g = groupMap.computeIfAbsent(gKey, k -> {
                CartViewVO.ShopGroup ng = new CartViewVO.ShopGroup();
                ng.setMerchantId(gKey);
                ng.setShopName(civ.getShopName());
                return ng;
            });
            g.getItems().add(civ);

            vo.setTotalCount(vo.getTotalCount() + 1);
            if (Integer.valueOf(1).equals(ci.getSelected()) && Boolean.TRUE.equals(civ.getAvailable())) {
                vo.setSelectedCount(vo.getSelectedCount() + 1);
                vo.setSelectedTotal(vo.getSelectedTotal().add(civ.getSubtotal()));
                g.setGroupTotal(g.getGroupTotal().add(civ.getSubtotal()));
            }
        }

        vo.setGroups(new ArrayList<>(groupMap.values()));
        return vo;
    }

    @Override
    public void update(Long userId, Long itemId, Integer quantity, Integer selected) {
        CartItem c = loadOwned(userId, itemId);
        LambdaUpdateWrapper<CartItem> uw = new LambdaUpdateWrapper<CartItem>()
                .eq(CartItem::getId, c.getId());
        boolean any = false;
        if (quantity != null) {
            if (quantity < 1 || quantity > 999) {
                throw BusinessException.of(ResultCode.BAD_REQUEST, "数量必须在 1~999");
            }
            uw.set(CartItem::getQuantity, quantity);
            any = true;
        }
        if (selected != null) {
            uw.set(CartItem::getSelected, selected != 0 ? 1 : 0);
            any = true;
        }
        if (!any) return;
        cartItemMapper.update(null, uw);
    }

    @Override
    public void remove(Long userId, Long itemId) {
        CartItem c = loadOwned(userId, itemId);
        cartItemMapper.deleteById(c.getId());
    }

    @Override
    public void selectAll(Long userId, boolean selected) {
        cartItemMapper.update(null, new LambdaUpdateWrapper<CartItem>()
                .eq(CartItem::getUserId, userId)
                .set(CartItem::getSelected, selected ? 1 : 0));
    }

    @Override
    public void clearSelected(Long userId) {
        cartItemMapper.delete(new LambdaQueryWrapper<CartItem>()
                .eq(CartItem::getUserId, userId)
                .eq(CartItem::getSelected, 1));
    }

    private CartItem loadOwned(Long userId, Long id) {
        CartItem c = cartItemMapper.selectById(id);
        if (c == null || !c.getUserId().equals(userId)) {
            throw BusinessException.of(ResultCode.CART_ITEM_NOT_EXIST);
        }
        return c;
    }
}
