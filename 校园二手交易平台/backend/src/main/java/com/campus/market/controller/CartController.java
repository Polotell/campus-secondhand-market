package com.campus.market.controller;

import com.campus.market.common.Result;
import com.campus.market.common.UserContext;
import com.campus.market.dto.CartAddDTO;
import com.campus.market.dto.CartUpdateDTO;
import com.campus.market.service.CartService;
import com.campus.market.vo.CartViewVO;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Map;

/**
 * 购物车
 * <p>需要登录即可访问；商家也可以加购，但禁止买自己店铺的商品（见 CartService）。</p>
 */
@Validated
@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @PostMapping
    public Result<Map<String, String>> add(@Valid @RequestBody CartAddDTO dto) {
        Long id = cartService.add(UserContext.getUserId(), dto.getProductId(), dto.getQuantity());
        return Result.success(Map.of("id", String.valueOf(id)));
    }

    @GetMapping
    public Result<CartViewVO> view() {
        return Result.success(cartService.view(UserContext.getUserId()));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody CartUpdateDTO dto) {
        cartService.update(UserContext.getUserId(), id, dto.getQuantity(), dto.getSelected());
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        cartService.remove(UserContext.getUserId(), id);
        return Result.success();
    }

    @PostMapping("/select-all")
    public Result<Void> selectAll(@RequestParam boolean selected) {
        cartService.selectAll(UserContext.getUserId(), selected);
        return Result.success();
    }

    @PostMapping("/clear-selected")
    public Result<Void> clearSelected() {
        cartService.clearSelected(UserContext.getUserId());
        return Result.success();
    }
}
