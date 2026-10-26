package com.example.wms.service;

import com.example.wms.common.PageResult;
import com.example.wms.dto.supplier.SupplierCreateRequest;
import com.example.wms.dto.supplier.SupplierQueryDTO;
import com.example.wms.dto.supplier.SupplierUpdateRequest;
import com.example.wms.vo.supplier.SupplierVO;

import java.util.List;

/** 供应商管理服务 */
public interface SupplierService {

    PageResult<SupplierVO> page(SupplierQueryDTO query);

    SupplierVO get(Long id);

    SupplierVO create(SupplierCreateRequest request);

    SupplierVO update(Long id, SupplierUpdateRequest request);

    /** 删除：校验无入库单引用，否则返回 2004 */
    void delete(Long id);

    void updateStatus(Long id, String status);

    List<SupplierVO> listEnabled();
}
