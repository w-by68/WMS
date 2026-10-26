package com.example.wms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.wms.common.BusinessException;
import com.example.wms.common.PageResult;
import com.example.wms.common.ResultCode;
import com.example.wms.dto.customer.CustomerCreateRequest;
import com.example.wms.dto.customer.CustomerQueryDTO;
import com.example.wms.dto.customer.CustomerUpdateRequest;
import com.example.wms.entity.Customer;
import com.example.wms.entity.OutboundOrder;
import com.example.wms.mapper.CustomerMapper;
import com.example.wms.mapper.OutboundOrderMapper;
import com.example.wms.service.CustomerService;
import com.example.wms.vo.customer.CustomerVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 客户管理服务实现。
 * 业务约束：编码唯一；删除前校验无出库单引用。
 */
@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerMapper customerMapper;
    private final OutboundOrderMapper outboundOrderMapper;

    @Override
    public PageResult<CustomerVO> page(CustomerQueryDTO query) {
        Page<Customer> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<Customer> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.and(w -> w.like(Customer::getCode, query.getKeyword())
                    .or().like(Customer::getName, query.getKeyword()));
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(Customer::getStatus, query.getStatus());
        }
        wrapper.orderByDesc(Customer::getCreateTime);

        IPage<Customer> result = customerMapper.selectPage(page, wrapper);
        Page<CustomerVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(this::toVO).toList());
        return PageResult.of(voPage);
    }

    @Override
    public CustomerVO get(Long id) {
        return toVO(getOrThrow(id));
    }

    @Override
    public CustomerVO create(CustomerCreateRequest request) {
        Long count = customerMapper.selectCount(
                new LambdaQueryWrapper<Customer>().eq(Customer::getCode, request.getCode()));
        if (count > 0) {
            throw new BusinessException(ResultCode.DUPLICATE_KEY, "客户编码已存在");
        }
        Customer customer = new Customer();
        BeanUtils.copyProperties(request, customer);
        if (!StringUtils.hasText(customer.getStatus())) {
            customer.setStatus("ENABLED");
        }
        customerMapper.insert(customer);
        return toVO(customer);
    }

    @Override
    public CustomerVO update(Long id, CustomerUpdateRequest request) {
        Customer customer = getOrThrow(id);
        BeanUtils.copyProperties(request, customer);
        customerMapper.updateById(customer);
        return toVO(customer);
    }

    @Override
    public void delete(Long id) {
        getOrThrow(id);
        Long orderCount = outboundOrderMapper.selectCount(
                new LambdaQueryWrapper<OutboundOrder>().eq(OutboundOrder::getCustomerId, id));
        if (orderCount > 0) {
            throw new BusinessException(ResultCode.DATA_REFERENCED, "该客户存在出库单记录，无法删除");
        }
        customerMapper.deleteById(id);
    }

    @Override
    public void updateStatus(Long id, String status) {
        getOrThrow(id);
        customerMapper.update(null, new LambdaUpdateWrapper<Customer>()
                .eq(Customer::getId, id)
                .set(Customer::getStatus, status));
    }

    @Override
    public List<CustomerVO> listEnabled() {
        return customerMapper.selectList(
                        new LambdaQueryWrapper<Customer>().eq(Customer::getStatus, "ENABLED")
                                .orderByAsc(Customer::getCode))
                .stream().map(this::toVO).toList();
    }

    private Customer getOrThrow(Long id) {
        Customer customer = customerMapper.selectById(id);
        if (customer == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "客户不存在");
        }
        return customer;
    }

    private CustomerVO toVO(Customer customer) {
        CustomerVO vo = new CustomerVO();
        BeanUtils.copyProperties(customer, vo);
        return vo;
    }
}
