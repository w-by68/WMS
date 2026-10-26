package com.example.wms.service;

import com.example.wms.common.PageResult;
import com.example.wms.dto.customer.CustomerCreateRequest;
import com.example.wms.dto.customer.CustomerQueryDTO;
import com.example.wms.dto.customer.CustomerUpdateRequest;
import com.example.wms.vo.customer.CustomerVO;

import java.util.List;

/** 客户管理服务 */
public interface CustomerService {

    PageResult<CustomerVO> page(CustomerQueryDTO query);

    CustomerVO get(Long id);

    CustomerVO create(CustomerCreateRequest request);

    CustomerVO update(Long id, CustomerUpdateRequest request);

    /** 删除：校验无出库单引用，否则返回 2004 */
    void delete(Long id);

    void updateStatus(Long id, String status);

    List<CustomerVO> listEnabled();
}
