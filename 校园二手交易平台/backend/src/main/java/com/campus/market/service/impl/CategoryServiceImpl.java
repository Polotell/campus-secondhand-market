package com.campus.market.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.market.entity.ProductCategory;
import com.campus.market.mapper.ProductCategoryMapper;
import com.campus.market.service.CategoryService;
import com.campus.market.vo.CategoryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 分类服务
 * <p>分类数据量很小（一般 &lt; 100 条），为了简单暂不加缓存，
 * 后续可加 Caffeine / Redis 做一级缓存。</p>
 */
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final ProductCategoryMapper categoryMapper;

    @Override
    public List<CategoryVO> tree() {
        List<ProductCategory> all = categoryMapper.selectList(
                new LambdaQueryWrapper<ProductCategory>()
                        .orderByAsc(ProductCategory::getParentId, ProductCategory::getSort));

        Map<Long, CategoryVO> map = new LinkedHashMap<>();
        List<CategoryVO> roots = new ArrayList<>();

        for (ProductCategory c : all) {
            CategoryVO v = CategoryVO.from(c);
            map.put(v.getId(), v);
        }
        // parent=0 的为一级
        for (CategoryVO v : map.values()) {
            if (v.getParentId() == null || v.getParentId() == 0L) {
                roots.add(v);
            } else {
                CategoryVO parent = map.get(v.getParentId());
                if (parent != null) parent.getChildren().add(v);
            }
        }
        return roots;
    }

    @Override
    public Map<Long, String> idNameMap() {
        return categoryMapper.selectList(null).stream()
                .collect(Collectors.toMap(ProductCategory::getId, ProductCategory::getName,
                        (a, b) -> a));
    }
}
