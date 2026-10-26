package com.example.wms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.wms.common.BusinessException;
import com.example.wms.common.PageResult;
import com.example.wms.common.ResultCode;
import com.example.wms.dto.alertrule.AlertRuleQueryDTO;
import com.example.wms.dto.alertrule.AlertRuleSaveRequest;
import com.example.wms.entity.InventoryAlertRule;
import com.example.wms.entity.Product;
import com.example.wms.entity.Warehouse;
import com.example.wms.mapper.InventoryAlertRuleMapper;
import com.example.wms.mapper.ProductMapper;
import com.example.wms.mapper.WarehouseMapper;
import com.example.wms.service.InventoryAlertRuleService;
import com.example.wms.vo.alertrule.AlertRuleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 预警规则服务实现。
 * 唯一约束：productId + warehouseId（warehouseId 为 NULL 表示全仓库通用）。
 * 匹配优先级（扫描时）：具体仓库规则 > 全仓库通用规则。
 */
@Service
@RequiredArgsConstructor
public class InventoryAlertRuleServiceImpl implements InventoryAlertRuleService {

    private final InventoryAlertRuleMapper alertRuleMapper;
    private final ProductMapper productMapper;
    private final WarehouseMapper warehouseMapper;

    @Override
    public PageResult<AlertRuleVO> page(AlertRuleQueryDTO query) {
        Page<InventoryAlertRule> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<InventoryAlertRule> wrapper = new LambdaQueryWrapper<>();
        if (query.getProductId() != null) {
            wrapper.eq(InventoryAlertRule::getProductId, query.getProductId());
        }
        if (query.getWarehouseId() != null) {
            wrapper.eq(InventoryAlertRule::getWarehouseId, query.getWarehouseId());
        }
        if (query.getEnabled() != null) {
            wrapper.eq(InventoryAlertRule::getEnabled, query.getEnabled());
        }
        wrapper.orderByDesc(InventoryAlertRule::getCreateTime);

        IPage<InventoryAlertRule> result = alertRuleMapper.selectPage(page, wrapper);
        Page<AlertRuleVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(this::toVO).toList());
        fillNames(voPage.getRecords());
        return PageResult.of(voPage);
    }

    @Override
    public AlertRuleVO get(Long id) {
        InventoryAlertRule rule = getOrThrow(id);
        AlertRuleVO vo = toVO(rule);
        fillNames(List.of(vo));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AlertRuleVO create(AlertRuleSaveRequest request) {
        validate(request);
        checkDuplicate(request.getProductId(), request.getWarehouseId(), null);
        InventoryAlertRule rule = new InventoryAlertRule();
        BeanUtils.copyProperties(request, rule);
        if (rule.getEnabled() == null) {
            rule.setEnabled(1);
        }
        alertRuleMapper.insert(rule);
        return get(rule.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AlertRuleVO update(Long id, AlertRuleSaveRequest request) {
        getOrThrow(id);
        validate(request);
        checkDuplicate(request.getProductId(), request.getWarehouseId(), id);
        InventoryAlertRule rule = new InventoryAlertRule();
        BeanUtils.copyProperties(request, rule);
        rule.setId(id);
        alertRuleMapper.updateById(rule);
        return get(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        getOrThrow(id);
        alertRuleMapper.deleteById(id);
    }

    @Override
    public void toggleEnabled(Long id, Integer enabled) {
        InventoryAlertRule rule = getOrThrow(id);
        rule.setEnabled(enabled == null || enabled == 1 ? 1 : 0);
        alertRuleMapper.updateById(rule);
    }

    // ==================== 私有方法 ====================

    private InventoryAlertRule getOrThrow(Long id) {
        InventoryAlertRule rule = alertRuleMapper.selectById(id);
        if (rule == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "预警规则不存在");
        }
        return rule;
    }

    private void validate(AlertRuleSaveRequest request) {
        if (productMapper.selectById(request.getProductId()) == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "商品不存在");
        }
        if (request.getWarehouseId() != null && warehouseMapper.selectById(request.getWarehouseId()) == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "仓库不存在");
        }
        if (request.getSafetyStock() == null || request.getSafetyStock() < 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "安全库存下限不能为负数");
        }
        if (request.getMaxStock() == null || request.getMaxStock() < 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "库存上限不能为负数");
        }
        if (request.getDeadStockDays() == null || request.getDeadStockDays() <= 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "呆滞天数必须大于 0");
        }
        if (request.getMaxStock() > 0 && request.getMaxStock() <= request.getSafetyStock()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "库存上限必须大于安全库存下限");
        }
    }

    /** productId + warehouseId 唯一校验（warehouseId 为 null 时用 IS NULL 匹配） */
    private void checkDuplicate(Long productId, Long warehouseId, Long excludeId) {
        LambdaQueryWrapper<InventoryAlertRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InventoryAlertRule::getProductId, productId);
        if (warehouseId == null) {
            wrapper.isNull(InventoryAlertRule::getWarehouseId);
        } else {
            wrapper.eq(InventoryAlertRule::getWarehouseId, warehouseId);
        }
        if (excludeId != null) {
            wrapper.ne(InventoryAlertRule::getId, excludeId);
        }
        if (alertRuleMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ResultCode.DUPLICATE_KEY, "该商品在对应仓库的预警规则已存在");
        }
    }

    private AlertRuleVO toVO(InventoryAlertRule rule) {
        AlertRuleVO vo = new AlertRuleVO();
        BeanUtils.copyProperties(rule, vo);
        vo.setGlobal(rule.getWarehouseId() == null);
        return vo;
    }

    private void fillNames(List<AlertRuleVO> vos) {
        if (vos == null || vos.isEmpty()) {
            return;
        }
        Set<Long> productIds = vos.stream().map(AlertRuleVO::getProductId).collect(Collectors.toSet());
        Set<Long> warehouseIds = vos.stream().map(AlertRuleVO::getWarehouseId)
                .filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Product> productMap = productMapper.selectBatchIds(productIds).stream()
                .collect(Collectors.toMap(Product::getId, p -> p));
        Map<Long, String> warehouseMap = warehouseIds.isEmpty() ? new HashMap<>()
                : warehouseMapper.selectBatchIds(warehouseIds).stream()
                        .collect(Collectors.toMap(Warehouse::getId, Warehouse::getName));
        vos.forEach(vo -> {
            Product p = productMap.get(vo.getProductId());
            if (p != null) {
                vo.setProductName(p.getName());
                vo.setSku(p.getSku());
            }
            vo.setWarehouseName(warehouseMap.get(vo.getWarehouseId()));
        });
    }
}
