package com.example.wms.vo.dashboard;

import lombok.Data;

/** 看板：预警统计 */
@Data
public class AlertStatsVO {

    /** 未处理预警总数 */
    private Long unresolved;

    /** 缺货预警数（未处理） */
    private Long shortage;

    /** 积压预警数（未处理） */
    private Long overstock;

    /** 呆滞预警数（未处理） */
    private Long deadStock;

    /** 严重级别数（未处理） */
    private Long critical;
}
