package com.example.wms.service;

import com.example.wms.common.PageResult;
import com.example.wms.dto.location.StorageLocationCreateRequest;
import com.example.wms.dto.location.StorageLocationQueryDTO;
import com.example.wms.dto.location.StorageLocationUpdateRequest;
import com.example.wms.vo.location.StorageLocationVO;

import java.util.List;

/** 库位管理服务 */
public interface StorageLocationService {

    PageResult<StorageLocationVO> page(StorageLocationQueryDTO query);

    StorageLocationVO get(Long id);

    StorageLocationVO create(StorageLocationCreateRequest request);

    StorageLocationVO update(Long id, StorageLocationUpdateRequest request);

    /** 删除：校验该库位下无库存，否则返回 2004 */
    void delete(Long id);

    /** 启用/禁用库位（人工操作，DISABLED 后不再允许入库） */
    void updateStatus(Long id, String status);

    /** 按仓库查询库位列表（用于出入库单据选择） */
    List<StorageLocationVO> listByWarehouse(Long warehouseId);
}
