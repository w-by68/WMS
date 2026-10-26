package com.example.wms.controller;

import com.example.wms.aspect.OperationLog;
import com.example.wms.common.R;
import com.example.wms.dto.category.ProductCategoryRequest;
import com.example.wms.service.ProductCategoryService;
import com.example.wms.vo.category.ProductCategoryVO;
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

import java.util.List;

/** 商品分类接口（ADMIN） */
@Tag(name = "商品分类")
@RestController
@RequestMapping("/api/product-categories")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class ProductCategoryController {

    private final ProductCategoryService productCategoryService;

    @Operation(summary = "商品分类树")
    @GetMapping("/tree")
    public R<List<ProductCategoryVO>> tree() {
        return R.ok(productCategoryService.tree());
    }

    @Operation(summary = "分类详情")
    @GetMapping("/{id}")
    public R<ProductCategoryVO> get(@PathVariable Long id) {
        return R.ok(productCategoryService.get(id));
    }

    @Operation(summary = "新建分类")
    @OperationLog(module = "商品分类", operation = "新建分类")
    @PostMapping
    public R<ProductCategoryVO> create(@Valid @RequestBody ProductCategoryRequest request) {
        return R.ok(productCategoryService.create(request));
    }

    @Operation(summary = "修改分类")
    @OperationLog(module = "商品分类", operation = "修改分类")
    @PutMapping("/{id}")
    public R<ProductCategoryVO> update(@PathVariable Long id, @Valid @RequestBody ProductCategoryRequest request) {
        return R.ok(productCategoryService.update(id, request));
    }

    @Operation(summary = "删除分类")
    @OperationLog(module = "商品分类", operation = "删除分类")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        productCategoryService.delete(id);
        return R.ok();
    }
}
