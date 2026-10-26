package com.example.wms.controller;

import com.example.wms.common.R;
import com.example.wms.service.DashboardService;
import com.example.wms.vo.dashboard.CategoryStockVO;
import com.example.wms.vo.dashboard.DashboardVO;
import com.example.wms.vo.dashboard.TrendPointVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 数据看板接口（ADMIN + USER 均可查看） */
@Tag(name = "数据看板")
@RestController
@RequestMapping("/api/dashboard")
@PreAuthorize("hasAnyRole('ADMIN','USER')")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "首页核心指标概览")
    @GetMapping("/overview")
    public R<DashboardVO> overview() {
        return R.ok(dashboardService.overview());
    }

    @Operation(summary = "出入库趋势（近 N 天，逐日补零）")
    @GetMapping("/trend")
    public R<List<TrendPointVO>> trend(@RequestParam(defaultValue = "30") int days) {
        return R.ok(dashboardService.trend(days));
    }

    @Operation(summary = "分类库存占比")
    @GetMapping("/category-stock")
    public R<List<CategoryStockVO>> categoryStock() {
        return R.ok(dashboardService.categoryStock());
    }
}
