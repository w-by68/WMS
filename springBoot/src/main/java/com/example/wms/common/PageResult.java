package com.example.wms.common;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * 统一分页结果：所有分页接口的 data 都使用此结构。
 * 字段与接口清单约定一致：list / total / pageNum / pageSize。
 *
 * @param <T> 行记录类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {

    /** 当前页数据 */
    private List<T> list;

    /** 总记录数 */
    private Long total;

    /** 当前页码（从 1 开始） */
    private Long pageNum;

    /** 每页大小 */
    private Long pageSize;

    /**
     * 由 MyBatis-Plus 的 IPage 转换而来，各分页 Service 统一走这里，
     * 避免每个查询接口重复手写字段映射。
     */
    public static <T> PageResult<T> of(IPage<T> page) {
        return new PageResult<>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    /** 空分页：用于防御性场景（如参数非法时直接返回空列表而非报错） */
    public static <T> PageResult<T> empty(long pageNum, long pageSize) {
        return new PageResult<>(Collections.emptyList(), 0L, pageNum, pageSize);
    }
}
