package com.example.wms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.wms.common.PageResult;
import com.example.wms.dto.inventory.StockTransactionQueryDTO;
import com.example.wms.entity.Product;
import com.example.wms.entity.StockTransaction;
import com.example.wms.entity.StorageLocation;
import com.example.wms.entity.Warehouse;
import com.example.wms.mapper.ProductMapper;
import com.example.wms.mapper.StockTransactionMapper;
import com.example.wms.mapper.StorageLocationMapper;
import com.example.wms.mapper.WarehouseMapper;
import com.example.wms.service.StockTransactionService;
import com.example.wms.vo.inventory.StockTransactionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** 库存流水查询服务实现 */
@Service
@RequiredArgsConstructor
public class StockTransactionServiceImpl implements StockTransactionService {

    private final StockTransactionMapper stockTransactionMapper;
    private final ProductMapper productMapper;
    private final WarehouseMapper warehouseMapper;
    private final StorageLocationMapper storageLocationMapper;

    @Override
    public PageResult<StockTransactionVO> page(StockTransactionQueryDTO query) {
        Page<StockTransaction> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<StockTransaction> wrapper = new LambdaQueryWrapper<>();
        if (query.getProductId() != null) {
            wrapper.eq(StockTransaction::getProductId, query.getProductId());
        }
        if (StringUtils.hasText(query.getBizType())) {
            wrapper.eq(StockTransaction::getBizType, query.getBizType());
        }
        if (StringUtils.hasText(query.getBizNo())) {
            wrapper.like(StockTransaction::getBizNo, query.getBizNo());
        }
        if (query.getStartTime() != null) {
            wrapper.ge(StockTransaction::getCreateTime, query.getStartTime());
        }
        if (query.getEndTime() != null) {
            wrapper.le(StockTransaction::getCreateTime, query.getEndTime());
        }
        wrapper.orderByDesc(StockTransaction::getCreateTime);

        IPage<StockTransaction> result = stockTransactionMapper.selectPage(page, wrapper);
        Page<StockTransactionVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(this::toVO).toList());
        fillNames(voPage.getRecords());
        return PageResult.of(voPage);
    }

    private StockTransactionVO toVO(StockTransaction tx) {
        StockTransactionVO vo = new StockTransactionVO();
        BeanUtils.copyProperties(tx, vo);
        return vo;
    }

    private void fillNames(List<StockTransactionVO> vos) {
        if (vos == null || vos.isEmpty()) {
            return;
        }
        Set<Long> productIds = vos.stream().map(StockTransactionVO::getProductId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> warehouseIds = vos.stream().map(StockTransactionVO::getWarehouseId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> locationIds = vos.stream().map(StockTransactionVO::getLocationId).filter(Objects::nonNull).collect(Collectors.toSet());

        Map<Long, String> productMap = productIds.isEmpty() ? Map.of()
                : productMapper.selectBatchIds(productIds).stream().collect(Collectors.toMap(Product::getId, Product::getName));
        Map<Long, String> warehouseMap = warehouseIds.isEmpty() ? Map.of()
                : warehouseMapper.selectBatchIds(warehouseIds).stream().collect(Collectors.toMap(Warehouse::getId, Warehouse::getName));
        Map<Long, String> locationMap = locationIds.isEmpty() ? Map.of()
                : storageLocationMapper.selectBatchIds(locationIds).stream().collect(Collectors.toMap(StorageLocation::getId, StorageLocation::getCode));

        vos.forEach(vo -> {
            vo.setProductName(productMap.get(vo.getProductId()));
            vo.setWarehouseName(warehouseMap.get(vo.getWarehouseId()));
            vo.setLocationCode(locationMap.get(vo.getLocationId()));
        });
    }
}
