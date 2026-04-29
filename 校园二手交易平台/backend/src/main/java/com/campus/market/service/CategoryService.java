package com.campus.market.service;

import com.campus.market.vo.CategoryVO;

import java.util.List;

public interface CategoryService {

    /** 返回树形分类列表（一级节点 -&gt; children 是二级） */
    List<CategoryVO> tree();

    /** 返回所有分类的 id→name 映射，便于商品列表回填分类名 */
    java.util.Map<Long, String> idNameMap();
}
