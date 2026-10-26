package com.example.wms.controller;

import com.example.wms.aspect.OperationLog;
import com.example.wms.common.PageResult;
import com.example.wms.common.R;
import com.example.wms.dto.alertrule.AlertRuleQueryDTO;
import com.example.wms.dto.alertrule.AlertRuleSaveRequest;
import com.example.wms.service.InventoryAlertRuleService;
import com.example.wms.vo.alertrule.AlertRuleVO;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 库存预警规则接口（仅 ADMIN） */
@Tag(name = "预警规则管理")
@RestController
@RequestMapping("/api/alert-rules")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class InventoryAlertRuleController {

    private final InventoryAlertRuleService alertRuleService;

    @Operation(summary = "预警规则分页列表")
    @GetMapping
    public R<PageResult<AlertRuleVO>> page(AlertRuleQueryDTO query) {
        return R.ok(alertRuleService.page(query));
    }

    @Operation(summary = "预警规则详情")
    @GetMapping("/{id}")
    public R<AlertRuleVO> get(@PathVariable Long id) {
        return R.ok(alertRuleService.get(id));
    }

    @Operation(summary = "新建预警规则")
    @OperationLog(module = "预警规则", operation = "新建预警规则")
    @PostMapping
    public R<AlertRuleVO> create(@Valid @RequestBody AlertRuleSaveRequest request) {
        return R.ok(alertRuleService.create(request));
    }

    @Operation(summary = "修改预警规则")
    @OperationLog(module = "预警规则", operation = "修改预警规则")
    @PutMapping("/{id}")
    public R<AlertRuleVO> update(@PathVariable Long id, @Valid @RequestBody AlertRuleSaveRequest request) {
        return R.ok(alertRuleService.update(id, request));
    }

    @Operation(summary = "删除预警规则")
    @OperationLog(module = "预警规则", operation = "删除预警规则")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        alertRuleService.delete(id);
        return R.ok();
    }

    @Operation(summary = "启用/停用预警规则")
    @OperationLog(module = "预警规则", operation = "切换规则启用状态")
    @PutMapping("/{id}/enabled")
    public R<Void> toggleEnabled(@PathVariable Long id, @RequestParam Integer enabled) {
        alertRuleService.toggleEnabled(id, enabled);
        return R.ok();
    }
}
