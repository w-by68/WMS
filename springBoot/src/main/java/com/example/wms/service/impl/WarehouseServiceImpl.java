package com.example.wms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.wms.common.BusinessException;
import com.example.wms.common.PageResult;
import com.example.wms.common.ResultCode;
import com.example.wms.dto.warehouse.WarehouseCreateRequest;
import com.example.wms.dto.warehouse.WarehouseQueryDTO;
import com.example.wms.dto.warehouse.WarehouseUpdateRequest;
import com.example.wms.entity.StorageLocation;
import com.example.wms.entity.Warehouse;
import com.example.wms.mapper.StorageLocationMapper;
import com.example.wms.mapper.WarehouseMapper;
import com.example.wms.service.WarehouseService;
import com.example.wms.vo.warehouse.WarehouseVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 仓库管理服务实现。
 * 业务约束：
 * 1. 仓库编码全局唯一（uk_code）；
 * 2. 删除前校验该仓库下无库位，否则返回 2004。
 */
@Service
@RequiredArgsConstructor
public class WarehouseServiceImpl implements WarehouseService {

    private final WarehouseMapper warehouseMapper;
    private final StorageLocationMapper storageLocationMapper;

    @Override
    public PageResult<WarehouseVO> page(WarehouseQueryDTO query) {
        Page<Warehouse> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<Warehouse> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.and(w -> w.like(Warehouse::getCode, query.getKeyword())
                    .or().like(Warehouse::getName, query.getKeyword()));
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(Warehouse::getStatus, query.getStatus());
        }
        wrapper.orderByDesc(Warehouse::getCreateTime);

        IPage<Warehouse> result = warehouseMapper.selectPage(page, wrapper);
        Page<WarehouseVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(this::toVO).toList());
        return PageResult.of(voPage);
    }

    @Override
    public WarehouseVO get(Long id) {
        return toVO(getOrThrow(id));
    }

    @Override
    public WarehouseVO create(WarehouseCreateRequest request) {
        Long count = warehouseMapper.selectCount(
                new LambdaQueryWrapper<Warehouse>().eq(Warehouse::getCode, request.getCode()));
        if (count > 0) {
            throw new BusinessException(ResultCode.DUPLICATE_KEY, "仓库编码已存在");
        }
        Warehouse warehouse = new Warehouse();
        BeanUtils.copyProperties(request, warehouse);
        if (!StringUtils.hasText(warehouse.getStatus())) {
            warehouse.setStatus("ENABLED");
        }
        warehouseMapper.insert(warehouse);
        return toVO(warehouse);
    }

    @Override
    public WarehouseVO update(Long id, WarehouseUpdateRequest request) {
        Warehouse warehouse = getOrThrow(id);
        BeanUtils.copyProperties(request, warehouse);
        warehouseMapper.updateById(warehouse);
        return toVO(warehouse);
    }

    @Override
    public void delete(Long id) {
        getOrThrow(id);
        // 引用校验：该仓库下是否还有库位
        Long locationCount = storageLocationMapper.selectCount(
                new LambdaQueryWrapper<StorageLocation>().eq(StorageLocation::getWarehouseId, id));
        if (locationCount > 0) {
            throw new BusinessException(ResultCode.DATA_REFERENCED, "该仓库下存在库位，无法删除");
        }
        warehouseMapper.deleteById(id);
    }

    @Override
    public void updateStatus(Long id, String status) {
        getOrThrow(id);
        warehouseMapper.update(null, new LambdaUpdateWrapper<Warehouse>()
                .eq(Warehouse::getId, id)
                .set(Warehouse::getStatus, status));
    }

    @Override
    public List<WarehouseVO> listEnabled() {
        List<Warehouse> list = warehouseMapper.selectList(
                new LambdaQueryWrapper<Warehouse>().eq(Warehouse::getStatus, "ENABLED")
                        .orderByAsc(Warehouse::getCode));
        return list.stream().map(this::toVO).toList();
    }

    private Warehouse getOrThrow(Long id) {
        Warehouse warehouse = warehouseMapper.selectById(id);
        if (warehouse == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "仓库不存在");
        }
        return warehouse;
    }

    private WarehouseVO toVO(Warehouse warehouse) {
        WarehouseVO vo = new WarehouseVO();
        BeanUtils.copyProperties(warehouse, vo);
        return vo;
    }
}
