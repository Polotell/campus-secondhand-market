package com.campus.market.controller;

import com.campus.market.annotation.RequiresRole;
import com.campus.market.common.PageResult;
import com.campus.market.common.Result;
import com.campus.market.common.UserContext;
import com.campus.market.common.enums.ProductStatus;
import com.campus.market.common.enums.UserRole;
import com.campus.market.dto.ProductCreateDTO;
import com.campus.market.service.ProductService;
import com.campus.market.vo.ProductListVO;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Collections;
import java.util.Map;

/**
 * 商家商品管理接口
 * <p>
 * 类级 {@link RequiresRole @RequiresRole(MERCHANT)} 保护所有端点，
 * 其他角色（包括 ADMIN 想发布商品也不行）一律 403。
 */
@Validated
@RestController
@RequestMapping("/merchant/products")
@RequiredArgsConstructor
@RequiresRole(UserRole.MERCHANT)
public class MerchantProductController {

    private final ProductService productService;

    /** 查看自己发布的商品 */
    @GetMapping
    public Result<PageResult<ProductListVO>> myProducts(
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(required = false, defaultValue = "1")  long pageNum,
            @RequestParam(required = false, defaultValue = "20") long pageSize) {
        Long mid = UserContext.getUserId();
        return Result.success(productService.listByMerchant(mid, status, pageNum, pageSize));
    }

    /** 发布新商品（进入 PENDING） */
    @PostMapping
    public Result<Map<String, String>> create(@Valid @RequestBody ProductCreateDTO dto) {
        Long mid = UserContext.getUserId();
        Long id  = productService.create(mid, dto);
        return Result.success(Collections.singletonMap("id", String.valueOf(id)));
    }

    /** 下架（ON_SALE → OFF_SHELF） */
    @PostMapping("/{id}/off-shelf")
    public Result<Void> offShelf(@PathVariable Long id) {
        productService.offShelf(UserContext.getUserId(), id);
        return Result.success();
    }

    /** 重新上架（OFF_SHELF → ON_SALE，无需二次审核） */
    @PostMapping("/{id}/on-shelf")
    public Result<Void> onShelf(@PathVariable Long id) {
        productService.onShelf(UserContext.getUserId(), id);
        return Result.success();
    }
}
