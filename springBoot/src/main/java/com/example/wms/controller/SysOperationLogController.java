package com.example.wms.controller;

import com.example.wms.common.PageResult;
import com.example.wms.common.R;
import com.example.wms.dto.log.OperationLogQueryDTO;
import com.example.wms.service.SysOperationLogService;
import com.example.wms.vo.log.OperationLogVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 操作日志查询接口（ADMIN 审计用，只读） */
@Tag(name = "操作日志")
@RestController
@RequestMapping("/api/operation-logs")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class SysOperationLogController {

    private final SysOperationLogService sysOperationLogService;

    @Operation(summary = "操作日志分页列表")
    @GetMapping
    public R<PageResult<OperationLogVO>> page(OperationLogQueryDTO query) {
        return R.ok(sysOperationLogService.page(query));
    }
}
