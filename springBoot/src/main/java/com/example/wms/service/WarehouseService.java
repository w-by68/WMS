package com.example.wms.service;

import com.example.wms.common.PageResult;
import com.example.wms.dto.warehouse.WarehouseCreateRequest;
import com.example.wms.dto.warehouse.WarehouseQueryDTO;
import com.example.wms.dto.warehouse.WarehouseUpdateRequest;
import com.example.wms.vo.warehouse.WarehouseVO;

import java.util.List;

/** 仓库管理服务 */
public interface WarehouseService {

    PageResult<WarehouseVO> page(WarehouseQueryDTO query);

    WarehouseVO get(Long id);

    WarehouseVO create(WarehouseCreateRequest request);

    WarehouseVO update(Long id, WarehouseUpdateRequest request);

    /** 删除：校验该仓库下没有库位，否则返回 2004 */
    void delete(Long id);

    void updateStatus(Long id, String status);

    /** 启用仓库下拉列表（所有 ENABLED） */
    List<WarehouseVO> listEnabled();
}
