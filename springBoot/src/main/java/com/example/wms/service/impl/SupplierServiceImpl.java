package com.example.wms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.wms.common.BusinessException;
import com.example.wms.common.PageResult;
import com.example.wms.common.ResultCode;
import com.example.wms.dto.supplier.SupplierCreateRequest;
import com.example.wms.dto.supplier.SupplierQueryDTO;
import com.example.wms.dto.supplier.SupplierUpdateRequest;
import com.example.wms.entity.InboundOrder;
import com.example.wms.entity.Supplier;
import com.example.wms.mapper.InboundOrderMapper;
import com.example.wms.mapper.SupplierMapper;
import com.example.wms.service.SupplierService;
import com.example.wms.vo.supplier.SupplierVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 供应商管理服务实现。
 * 业务约束：编码唯一；删除前校验无入库单引用。
 */
@Service
@RequiredArgsConstructor
public class SupplierServiceImpl implements SupplierService {

    private final SupplierMapper supplierMapper;
    private final InboundOrderMapper inboundOrderMapper;

    @Override
    public PageResult<SupplierVO> page(SupplierQueryDTO query) {
        Page<Supplier> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<Supplier> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.and(w -> w.like(Supplier::getCode, query.getKeyword())
                    .or().like(Supplier::getName, query.getKeyword()));
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(Supplier::getStatus, query.getStatus());
        }
        wrapper.orderByDesc(Supplier::getCreateTime);

        IPage<Supplier> result = supplierMapper.selectPage(page, wrapper);
        Page<SupplierVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(this::toVO).toList());
        return PageResult.of(voPage);
    }

    @Override
    public SupplierVO get(Long id) {
        return toVO(getOrThrow(id));
    }

    @Override
    public SupplierVO create(SupplierCreateRequest request) {
        Long count = supplierMapper.selectCount(
                new LambdaQueryWrapper<Supplier>().eq(Supplier::getCode, request.getCode()));
        if (count > 0) {
            throw new BusinessException(ResultCode.DUPLICATE_KEY, "供应商编码已存在");
        }
        Supplier supplier = new Supplier();
        BeanUtils.copyProperties(request, supplier);
        if (!StringUtils.hasText(supplier.getStatus())) {
            supplier.setStatus("ENABLED");
        }
        supplierMapper.insert(supplier);
        return toVO(supplier);
    }

    @Override
    public SupplierVO update(Long id, SupplierUpdateRequest request) {
        Supplier supplier = getOrThrow(id);
        BeanUtils.copyProperties(request, supplier);
        supplierMapper.updateById(supplier);
        return toVO(supplier);
    }

    @Override
    public void delete(Long id) {
        getOrThrow(id);
        Long orderCount = inboundOrderMapper.selectCount(
                new LambdaQueryWrapper<InboundOrder>().eq(InboundOrder::getSupplierId, id));
        if (orderCount > 0) {
            throw new BusinessException(ResultCode.DATA_REFERENCED, "该供应商存在入库单记录，无法删除");
        }
        supplierMapper.deleteById(id);
    }

    @Override
    public void updateStatus(Long id, String status) {
        getOrThrow(id);
        supplierMapper.update(null, new LambdaUpdateWrapper<Supplier>()
                .eq(Supplier::getId, id)
                .set(Supplier::getStatus, status));
    }

    @Override
    public List<SupplierVO> listEnabled() {
        return supplierMapper.selectList(
                        new LambdaQueryWrapper<Supplier>().eq(Supplier::getStatus, "ENABLED")
                                .orderByAsc(Supplier::getCode))
                .stream().map(this::toVO).toList();
    }

    private Supplier getOrThrow(Long id) {
        Supplier supplier = supplierMapper.selectById(id);
        if (supplier == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "供应商不存在");
        }
        return supplier;
    }

    private SupplierVO toVO(Supplier supplier) {
        SupplierVO vo = new SupplierVO();
        BeanUtils.copyProperties(supplier, vo);
        return vo;
    }
}
