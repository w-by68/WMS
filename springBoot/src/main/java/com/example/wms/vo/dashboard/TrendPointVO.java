package com.example.wms.vo.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 出入库趋势的单个数据点（按天）。
 * 前端折线图的 x 轴用 date，两条线分别用 inboundQty / outboundQty。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrendPointVO {

    /** 日期，格式 yyyy-MM-dd */
    private String date;

    /** 当日入库数量合计（来自 stock_transaction 中 bizType=INBOUND 的 changeQuantity） */
    private Integer inboundQty;

    /** 当日出库数量合计（取 bizType=OUTBOUND 的 changeQuantity 绝对值） */
    private Integer outboundQty;
}
