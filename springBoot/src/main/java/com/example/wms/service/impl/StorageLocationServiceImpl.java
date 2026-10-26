package com.example.wms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.wms.common.BusinessException;
import com.example.wms.common.PageResult;
import com.example.wms.common.ResultCode;
import com.example.wms.dto.location.StorageLocationCreateRequest;
import com.example.wms.dto.location.StorageLocationQueryDTO;
import com.example.wms.dto.location.StorageLocationUpdateRequest;
import com.example.wms.entity.Inventory;
import com.example.wms.entity.StorageLocation;
import com.example.wms.entity.Warehouse;
import com.example.wms.mapper.InventoryMapper;
import com.example.wms.mapper.StorageLocationMapper;
import com.example.wms.mapper.WarehouseMapper;
import com.example.wms.service.StorageLocationService;
import com.example.wms.vo.location.StorageLocationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 库位管理服务实现。
 * 业务约束：
 * 1. 新建时校验仓库存在；
 * 2. 同一仓库内库位编码唯一（uk_wh_code）；
 * 3. 删除前校验该库位下无库存，否则返回 2004。
 */
@Service
@RequiredArgsConstructor
public class StorageLocationServiceImpl implements StorageLocationService {

    private final StorageLocationMapper storageLocationMapper;
    private final WarehouseMapper warehouseMapper;
    private final InventoryMapper inventoryMapper;

    @Override
    public PageResult<StorageLocationVO> page(StorageLocationQueryDTO query) {
        Page<StorageLocation> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<StorageLocation> wrapper = new LambdaQueryWrapper<>();
        if (query.getWarehouseId() != null) {
            wrapper.eq(StorageLocation::getWarehouseId, query.getWarehouseId());
        }
        if (StringUtils.hasText(query.getZone())) {
            wrapper.eq(StorageLocation::getZone, query.getZone());
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(StorageLocation::getStatus, query.getStatus());
        }
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.like(StorageLocation::getCode, query.getKeyword());
        }
        wrapper.orderByAsc(StorageLocation::getWarehouseId).orderByAsc(StorageLocation::getCode);

        IPage<StorageLocation> result = storageLocationMapper.selectPage(page, wrapper);
        Page<StorageLocationVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(this::toVO).toList());
        fillWarehouseName(voPage.getRecords());
        return PageResult.of(voPage);
    }

    @Override
    public StorageLocationVO get(Long id) {
        StorageLocationVO vo = toVO(getOrThrow(id));
        fillWarehouseName(List.of(vo));
        return vo;
    }

    @Override
    public StorageLocationVO create(StorageLocationCreateRequest request) {
        // 校验仓库存在
        Warehouse warehouse = warehouseMapper.selectById(request.getWarehouseId());
        if (warehouse == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "所属仓库不存在");
        }
        // 仓库内编码唯一
        Long count = storageLocationMapper.selectCount(new LambdaQueryWrapper<StorageLocation>()
                .eq(StorageLocation::getWarehouseId, request.getWarehouseId())
                .eq(StorageLocation::getCode, request.getCode()));
        if (count > 0) {
            throw new BusinessException(ResultCode.DUPLICATE_KEY, "该仓库下库位编码已存在");
        }
        StorageLocation location = new StorageLocation();
        BeanUtils.copyProperties(request, location);
        location.setStatus("EMPTY");
        storageLocationMapper.insert(location);
        return toVO(location);
    }

    @Override
    public StorageLocationVO update(Long id, StorageLocationUpdateRequest request) {
        StorageLocation location = getOrThrow(id);
        BeanUtils.copyProperties(request, location);
        storageLocationMapper.updateById(location);
        return toVO(location);
    }

    @Override
    public void delete(Long id) {
        getOrThrow(id);
        // 引用校验：该库位下是否还有库存
        Long invCount = inventoryMapper.selectCount(
                new LambdaQueryWrapper<Inventory>().eq(Inventory::getLocationId, id));
        if (invCount > 0) {
            throw new BusinessException(ResultCode.DATA_REFERENCED, "该库位下存在库存，无法删除");
        }
        storageLocationMapper.deleteById(id);
    }

    @Override
    public void updateStatus(Long id, String status) {
        getOrThrow(id);
        storageLocationMapper.update(null, new LambdaUpdateWrapper<StorageLocation>()
                .eq(StorageLocation::getId, id)
                .set(StorageLocation::getStatus, status));
    }

    @Override
    public List<StorageLocationVO> listByWarehouse(Long warehouseId) {
        List<StorageLocation> list = storageLocationMapper.selectList(
                new LambdaQueryWrapper<StorageLocation>()
                        .eq(StorageLocation::getWarehouseId, warehouseId)
                        .ne(StorageLocation::getStatus, "DISABLED")
                        .orderByAsc(StorageLocation::getCode));
        List<StorageLocationVO> vos = list.stream().map(this::toVO).toList();
        fillWarehouseName(vos);
        return vos;
    }

    private StorageLocation getOrThrow(Long id) {
        StorageLocation location = storageLocationMapper.selectById(id);
        if (location == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "库位不存在");
        }
        return location;
    }

    private StorageLocationVO toVO(StorageLocation location) {
        StorageLocationVO vo = new StorageLocationVO();
        BeanUtils.copyProperties(location, vo);
        return vo;
    }

    /** 批量回填仓库名称，避免 N+1 查询 */
    private void fillWarehouseName(List<StorageLocationVO> vos) {
        if (vos == null || vos.isEmpty()) {
            return;
        }
        Set<Long> whIds = vos.stream().map(StorageLocationVO::getWarehouseId)
                .filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        if (whIds.isEmpty()) {
            return;
        }
        Map<Long, String> whNameMap = warehouseMapper.selectBatchIds(whIds).stream()
                .collect(Collectors.toMap(Warehouse::getId, Warehouse::getName));
        vos.forEach(vo -> vo.setWarehouseName(whNameMap.get(vo.getWarehouseId())));
    }
}
