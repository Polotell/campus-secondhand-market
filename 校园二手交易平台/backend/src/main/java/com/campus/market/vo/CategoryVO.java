package com.campus.market.vo;

import com.campus.market.entity.ProductCategory;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 分类树节点（支持任意层级，但当前只有两级）
 */
@Data
public class CategoryVO implements Serializable {

    private Long   id;
    private String name;
    private Long   parentId;
    private Integer sort;
    private String icon;
    private List<CategoryVO> children = new ArrayList<>();

    public static CategoryVO from(ProductCategory c) {
        CategoryVO v = new CategoryVO();
        v.setId(c.getId());
        v.setName(c.getName());
        v.setParentId(c.getParentId());
        v.setSort(c.getSort());
        v.setIcon(c.getIcon());
        return v;
    }
}
