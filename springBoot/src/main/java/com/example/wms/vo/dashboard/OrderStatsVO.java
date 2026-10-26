package com.example.wms.vo.dashboard;

import lombok.Data;

/** 看板：出入库单据统计 */
@Data
public class OrderStatsVO {

    /** 待审核数量 */
    private Long pending;

    /** 今日新增数量 */
    private Long today;
}
