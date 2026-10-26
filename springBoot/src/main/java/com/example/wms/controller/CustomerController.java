package com.example.wms.controller;

import com.example.wms.aspect.OperationLog;
import com.example.wms.common.PageResult;
import com.example.wms.common.R;
import com.example.wms.dto.customer.CustomerCreateRequest;
import com.example.wms.dto.customer.CustomerQueryDTO;
import com.example.wms.dto.customer.CustomerUpdateRequest;
import com.example.wms.service.CustomerService;
import com.example.wms.vo.customer.CustomerVO;
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

import java.util.List;

/** 客户管理接口（ADMIN） */
@Tag(name = "客户管理")
@RestController
@RequestMapping("/api/customers")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @Operation(summary = "客户分页列表")
    @GetMapping
    public R<PageResult<CustomerVO>> page(CustomerQueryDTO query) {
        return R.ok(customerService.page(query));
    }

    @Operation(summary = "客户详情")
    @GetMapping("/{id}")
    public R<CustomerVO> get(@PathVariable Long id) {
        return R.ok(customerService.get(id));
    }

    @Operation(summary = "启用客户下拉列表")
    // 制单时需要，放开给普通用户；本类其余写接口仍受类级 ADMIN 限制
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @GetMapping("/options")
    public R<List<CustomerVO>> options() {
        return R.ok(customerService.listEnabled());
    }

    @Operation(summary = "创建客户")
    @OperationLog(module = "客户管理", operation = "创建客户")
    @PostMapping
    public R<CustomerVO> create(@Valid @RequestBody CustomerCreateRequest request) {
        return R.ok(customerService.create(request));
    }

    @Operation(summary = "修改客户")
    @OperationLog(module = "客户管理", operation = "修改客户")
    @PutMapping("/{id}")
    public R<CustomerVO> update(@PathVariable Long id, @Valid @RequestBody CustomerUpdateRequest request) {
        return R.ok(customerService.update(id, request));
    }

    @Operation(summary = "删除客户")
    @OperationLog(module = "客户管理", operation = "删除客户")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        customerService.delete(id);
        return R.ok();
    }

    @Operation(summary = "启用/停用客户")
    @OperationLog(module = "客户管理", operation = "切换客户状态")
    @PutMapping("/{id}/status")
    public R<Void> updateStatus(@PathVariable Long id, @RequestParam String status) {
        customerService.updateStatus(id, status);
        return R.ok();
    }
}
