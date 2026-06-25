package com.drivingschool.common.result;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 分页查询结果封装类。
 * <p>
 * 统一包装 MyBatis-Plus 分页查询的结果，提供标准化的分页响应格式。
 * 前端可根据 total 和 pages 实现分页导航，根据 records 获取数据列表。
 * </p>
 * <p>
 * 响应示例：
 * {
 *   "records": [...],    // 当前页数据列表
 *   "total": 100,        // 总记录数
 *   "pageNum": 1,        // 当前页码
 *   "pageSize": 10,      // 每页大小
 *   "pages": 10          // 总页数
 * }
 * </p>
 *
 * @param <T> 数据记录的类型
 */
@Data
public class PageResult<T> implements Serializable {

    /** 当前页的数据记录列表 */
    private List<T> records;

    /** 符合条件的总记录数 */
    private long total;

    /** 当前页码（从 1 开始） */
    private long pageNum;

    /** 每页显示的记录数 */
    private long pageSize;

    /** 总页数 */
    private long pages;

    /**
     * 将 MyBatis-Plus 的 IPage 分页结果转换为 PageResult。
     * <p>
     * 静态工厂方法，简化分页结果的转换操作。
     * </p>
     *
     * @param page MyBatis-Plus 分页查询结果
     * @param <T>  数据记录的类型
     * @return 包装后的 PageResult 对象
     */
    public static <T> PageResult<T> of(IPage<T> page) {
        PageResult<T> result = new PageResult<>();
        result.setRecords(page.getRecords());   // 当前页数据
        result.setTotal(page.getTotal());       // 总记录数
        result.setPageNum(page.getCurrent());   // 当前页码
        result.setPageSize(page.getSize());     // 每页大小
        result.setPages(page.getPages());       // 总页数
        return result;
    }
}
