package com.example.wms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.entity.StockTransaction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** 库存流水表 Mapper */
@Mapper
public interface StockTransactionMapper extends BaseMapper<StockTransaction> {

    /** 统计今日已生成的流水数量，用于生成 ST + yyyyMMdd + 3位序号 的流水号 */
    @Select("SELECT COUNT(*) FROM stock_transaction WHERE transaction_no LIKE CONCAT('ST', #{dateStr}, '%')")
    int countTodayTransactions(@Param("dateStr") String dateStr);

    /**
     * 按天汇总出入库数量（看板趋势图）。
     * INBOUND 取 change_quantity 正值合计；OUTBOUND 取 -change_quantity（流水记录为负）取绝对值。
     * 返回 bizDate / inboundQty / outboundQty，缺失日期由 Service 层补零。
     */
    @Select("SELECT DATE_FORMAT(create_time, '%Y-%m-%d') AS bizDate, " +
            "SUM(CASE WHEN biz_type = 'INBOUND' THEN change_quantity ELSE 0 END) AS inboundQty, " +
            "SUM(CASE WHEN biz_type = 'OUTBOUND' THEN -change_quantity ELSE 0 END) AS outboundQty " +
            "FROM stock_transaction " +
            "WHERE create_time >= #{startTime} " +
            "GROUP BY DATE_FORMAT(create_time, '%Y-%m-%d')")
    List<Map<String, Object>> trendByDay(@Param("startTime") LocalDateTime startTime);
}
