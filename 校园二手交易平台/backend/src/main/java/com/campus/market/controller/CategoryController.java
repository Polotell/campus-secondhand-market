package com.campus.market.controller;

import com.campus.market.common.Result;
import com.campus.market.service.CategoryService;
import com.campus.market.vo.CategoryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 商品分类（公开，匿名可访问）
 */
@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    /** 分类树 */
    @GetMapping
    public Result<List<CategoryVO>> tree() {
        return Result.success(categoryService.tree());
    }
}
