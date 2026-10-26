package com.example.wms.service;

import com.example.wms.vo.dashboard.CategoryStockVO;
import com.example.wms.vo.dashboard.DashboardVO;
import com.example.wms.vo.dashboard.TrendPointVO;

import java.util.List;

/** 数据看板服务 */
public interface DashboardService {

    /** 汇总首页核心指标 */
    DashboardVO overview();

    /** 近 N 天的出入库数量趋势（逐日补零，按日期升序） */
    List<TrendPointVO> trend(int days);

    /** 分类库存占比（账面库存合计，按数量降序） */
    List<CategoryStockVO> categoryStock();
}
