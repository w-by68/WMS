package com.example.wms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.wms.common.BusinessException;
import com.example.wms.common.PageResult;
import com.example.wms.common.ResultCode;
import com.example.wms.dto.alert.AlertHandleRequest;
import com.example.wms.dto.alert.AlertQueryDTO;
import com.example.wms.entity.InventoryAlert;
import com.example.wms.entity.Product;
import com.example.wms.entity.SysUser;
import com.example.wms.entity.Warehouse;
import com.example.wms.mapper.InventoryAlertMapper;
import com.example.wms.mapper.ProductMapper;
import com.example.wms.mapper.SysUserMapper;
import com.example.wms.mapper.WarehouseMapper;
import com.example.wms.security.SecurityUtils;
import com.example.wms.service.InventoryAlertService;
import com.example.wms.vo.alert.AlertVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 预警记录服务实现。
 * 处理规则：UNRESOLVED 可被标记为 RESOLVED / IGNORED，终态后不可再改。
 */
@Service
@RequiredArgsConstructor
public class InventoryAlertServiceImpl implements InventoryAlertService {

    private final InventoryAlertMapper alertMapper;
    private final ProductMapper productMapper;
    private final WarehouseMapper warehouseMapper;
    private final SysUserMapper sysUserMapper;

    @Override
    public PageResult<AlertVO> page(AlertQueryDTO query) {
        Page<InventoryAlert> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<InventoryAlert> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getAlertType())) {
            wrapper.eq(InventoryAlert::getAlertType, query.getAlertType());
        }
        if (StringUtils.hasText(query.getLevel())) {
            wrapper.eq(InventoryAlert::getLevel, query.getLevel());
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(InventoryAlert::getStatus, query.getStatus());
        }
        if (query.getProductId() != null) {
            wrapper.eq(InventoryAlert::getProductId, query.getProductId());
        }
        if (query.getWarehouseId() != null) {
            wrapper.eq(InventoryAlert::getWarehouseId, query.getWarehouseId());
        }
        wrapper.orderByDesc(InventoryAlert::getCreateTime);

        IPage<InventoryAlert> result = alertMapper.selectPage(page, wrapper);
        Page<AlertVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(this::toVO).toList());
        fillNames(voPage.getRecords());
        return PageResult.of(voPage);
    }

    @Override
    public AlertVO get(Long id) {
        InventoryAlert alert = alertMapper.selectById(id);
        if (alert == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "预警记录不存在");
        }
        AlertVO vo = toVO(alert);
        fillNames(List.of(vo));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handle(Long id, AlertHandleRequest request) {
        InventoryAlert alert = alertMapper.selectById(id);
        if (alert == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "预警记录不存在");
        }
        if (!"UNRESOLVED".equals(alert.getStatus())) {
            throw new BusinessException(ResultCode.ORDER_STATE_ERROR, "该预警已处理，不可重复操作");
        }
        String target = request.getStatus();
        if (!"RESOLVED".equals(target) && !"IGNORED".equals(target)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "处理状态只能为 RESOLVED 或 IGNORED");
        }
        alert.setStatus(target);
        alert.setHandlerId(SecurityUtils.getCurrentUserId());
        alert.setHandleTime(LocalDateTime.now());
        alert.setHandleRemark(request.getHandleRemark());
        alertMapper.updateById(alert);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchHandle(AlertHandleRequest request) {
        String target = request.getStatus();
        if (!"RESOLVED".equals(target) && !"IGNORED".equals(target)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "处理状态只能为 RESOLVED 或 IGNORED");
        }
        Long handlerId = SecurityUtils.getCurrentUserId();
        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<InventoryAlert> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(InventoryAlert::getStatus, "UNRESOLVED")
                .set(InventoryAlert::getStatus, target)
                .set(InventoryAlert::getHandlerId, handlerId)
                .set(InventoryAlert::getHandleTime, now)
                .set(InventoryAlert::getHandleRemark, request.getHandleRemark());
        return alertMapper.update(null, wrapper);
    }

    // ==================== 私有方法 ====================

    private AlertVO toVO(InventoryAlert alert) {
        AlertVO vo = new AlertVO();
        BeanUtils.copyProperties(alert, vo);
        return vo;
    }

    private void fillNames(List<AlertVO> vos) {
        if (vos == null || vos.isEmpty()) {
            return;
        }
        Set<Long> productIds = vos.stream().map(AlertVO::getProductId).collect(Collectors.toSet());
        Set<Long> warehouseIds = vos.stream().map(AlertVO::getWarehouseId).collect(Collectors.toSet());
        Set<Long> handlerIds = vos.stream().map(AlertVO::getHandlerId)
                .filter(java.util.Objects::nonNull).collect(Collectors.toSet());

        Map<Long, Product> productMap = productMapper.selectBatchIds(productIds).stream()
                .collect(Collectors.toMap(Product::getId, p -> p));
        Map<Long, String> warehouseMap = warehouseMapper.selectBatchIds(warehouseIds).stream()
                .collect(Collectors.toMap(Warehouse::getId, Warehouse::getName));
        Map<Long, String> handlerMap = handlerIds.isEmpty() ? new HashMap<>()
                : sysUserMapper.selectBatchIds(handlerIds).stream()
                        .collect(Collectors.toMap(SysUser::getId, SysUser::getRealName));

        vos.forEach(vo -> {
            Product p = productMap.get(vo.getProductId());
            if (p != null) {
                vo.setProductName(p.getName());
                vo.setSku(p.getSku());
            }
            vo.setWarehouseName(warehouseMap.get(vo.getWarehouseId()));
            vo.setHandlerName(handlerMap.get(vo.getHandlerId()));
        });
    }
}
