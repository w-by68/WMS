package com.example.wms.controller;

import com.example.wms.aspect.OperationLog;
import com.example.wms.common.PageResult;
import com.example.wms.common.R;
import com.example.wms.dto.inbound.AuditRequest;
import com.example.wms.dto.inbound.InboundOrderQueryDTO;
import com.example.wms.dto.inbound.InboundOrderSaveRequest;
import com.example.wms.service.InboundOrderService;
import com.example.wms.vo.inbound.InboundOrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 入库单接口（ADMIN + USER；审核仅 ADMIN） */
@Tag(name = "入库单管理")
@RestController
@RequestMapping("/api/inbound-orders")
@PreAuthorize("hasAnyRole('ADMIN','USER')")
@RequiredArgsConstructor
public class InboundOrderController {

    private final InboundOrderService inboundOrderService;

    @Operation(summary = "入库单分页列表")
    @GetMapping
    public R<PageResult<InboundOrderVO>> page(InboundOrderQueryDTO query) {
        return R.ok(inboundOrderService.page(query));
    }

    @Operation(summary = "入库单详情")
    @GetMapping("/{id}")
    public R<InboundOrderVO> get(@PathVariable Long id) {
        return R.ok(inboundOrderService.get(id));
    }

    @Operation(summary = "新建入库单")
    @OperationLog(module = "入库单", operation = "新建入库单")
    @PostMapping
    public R<InboundOrderVO> create(@Valid @RequestBody InboundOrderSaveRequest request) {
        return R.ok(inboundOrderService.create(request));
    }

    @Operation(summary = "修改入库单")
    @OperationLog(module = "入库单", operation = "修改入库单")
    @PutMapping("/{id}")
    public R<InboundOrderVO> update(@PathVariable Long id, @Valid @RequestBody InboundOrderSaveRequest request) {
        return R.ok(inboundOrderService.update(id, request));
    }

    @Operation(summary = "删除入库单")
    @OperationLog(module = "入库单", operation = "删除入库单")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        inboundOrderService.delete(id);
        return R.ok();
    }

    @Operation(summary = "提交审核")
    @OperationLog(module = "入库单", operation = "提交审核")
    @PutMapping("/{id}/submit")
    public R<Void> submit(@PathVariable Long id) {
        inboundOrderService.submit(id);
        return R.ok();
    }

    @Operation(summary = "审核通过（增加库存）")
    @OperationLog(module = "入库单", operation = "审核通过")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/approve")
    public R<Void> approve(@PathVariable Long id) {
        inboundOrderService.approve(id);
        return R.ok();
    }

    @Operation(summary = "驳回")
    @OperationLog(module = "入库单", operation = "驳回")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/reject")
    public R<Void> reject(@PathVariable Long id, @RequestBody AuditRequest request) {
        inboundOrderService.reject(id, request);
        return R.ok();
    }

    @Operation(summary = "作废")
    @OperationLog(module = "入库单", operation = "作废")
    @PutMapping("/{id}/cancel")
    public R<Void> cancel(@PathVariable Long id) {
        inboundOrderService.cancel(id);
        return R.ok();
    }
}
