package com.example.wms.controller;

import com.example.wms.common.PageResult;
import com.example.wms.common.R;
import com.example.wms.dto.inventory.StockTransactionQueryDTO;
import com.example.wms.service.StockTransactionService;
import com.example.wms.vo.inventory.StockTransactionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 库存流水查询接口（ADMIN + USER） */
@Tag(name = "库存流水")
@RestController
@RequestMapping("/api/stock-transactions")
@PreAuthorize("hasAnyRole('ADMIN','USER')")
@RequiredArgsConstructor
public class StockTransactionController {

    private final StockTransactionService stockTransactionService;

    @Operation(summary = "库存流水分页列表")
    @GetMapping
    public R<PageResult<StockTransactionVO>> page(StockTransactionQueryDTO query) {
        return R.ok(stockTransactionService.page(query));
    }
}
