package com.example.wms.controller;

import com.example.wms.aspect.OperationLog;
import com.example.wms.common.PageResult;
import com.example.wms.common.R;
import com.example.wms.dto.product.ProductCreateRequest;
import com.example.wms.dto.product.ProductQueryDTO;
import com.example.wms.dto.product.ProductUpdateRequest;
import com.example.wms.service.ProductService;
import com.example.wms.vo.product.ProductVO;
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

/** 商品管理接口（ADMIN） */
@Tag(name = "商品管理")
@RestController
@RequestMapping("/api/products")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @Operation(summary = "商品分页列表")
    @GetMapping
    public R<PageResult<ProductVO>> page(ProductQueryDTO query) {
        return R.ok(productService.page(query));
    }

    @Operation(summary = "商品详情")
    @GetMapping("/{id}")
    public R<ProductVO> get(@PathVariable Long id) {
        return R.ok(productService.get(id));
    }

    @Operation(summary = "启用商品下拉列表")
    // 制单时需要，放开给普通用户；本类其余写接口仍受类级 ADMIN 限制
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @GetMapping("/options")
    public R<List<ProductVO>> options() {
        return R.ok(productService.listEnabled());
    }

    @Operation(summary = "创建商品")
    @OperationLog(module = "商品管理", operation = "创建商品")
    @PostMapping
    public R<ProductVO> create(@Valid @RequestBody ProductCreateRequest request) {
        return R.ok(productService.create(request));
    }

    @Operation(summary = "修改商品")
    @OperationLog(module = "商品管理", operation = "修改商品")
    @PutMapping("/{id}")
    public R<ProductVO> update(@PathVariable Long id, @Valid @RequestBody ProductUpdateRequest request) {
        return R.ok(productService.update(id, request));
    }

    @Operation(summary = "删除商品")
    @OperationLog(module = "商品管理", operation = "删除商品")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return R.ok();
    }

    @Operation(summary = "启用/停用商品")
    @OperationLog(module = "商品管理", operation = "切换商品状态")
    @PutMapping("/{id}/status")
    public R<Void> updateStatus(@PathVariable Long id, @RequestParam String status) {
        productService.updateStatus(id, status);
        return R.ok();
    }
}
