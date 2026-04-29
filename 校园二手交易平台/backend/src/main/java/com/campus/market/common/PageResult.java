package com.campus.market.common;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;

import java.util.List;

/**
 * 分页结果封装（对接 MyBatis Plus 的 {@link IPage}，前后端字段保持一致）
 */
@Data
public class PageResult<T> {

    /** 当前页数据 */
    private List<T> records;
    /** 总记录数 */
    private Long total;
    /** 当前页码（从 1 开始） */
    private Long pageNum;
    /** 每页大小 */
    private Long pageSize;

    public static <T> PageResult<T> of(IPage<T> page) {
        PageResult<T> r = new PageResult<>();
        r.setRecords(page.getRecords());
        r.setTotal(page.getTotal());
        r.setPageNum(page.getCurrent());
        r.setPageSize(page.getSize());
        return r;
    }

    public static <T, R> PageResult<R> of(IPage<T> page, List<R> records) {
        PageResult<R> r = new PageResult<>();
        r.setRecords(records);
        r.setTotal(page.getTotal());
        r.setPageNum(page.getCurrent());
        r.setPageSize(page.getSize());
        return r;
    }
}
