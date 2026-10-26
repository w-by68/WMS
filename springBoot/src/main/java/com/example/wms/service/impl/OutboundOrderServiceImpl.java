package com.example.wms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.wms.common.BusinessException;
import com.example.wms.common.PageResult;
import com.example.wms.common.ResultCode;
import com.example.wms.dto.inbound.AuditRequest;
import com.example.wms.dto.outbound.OutboundOrderItemDTO;
import com.example.wms.dto.outbound.OutboundOrderQueryDTO;
import com.example.wms.dto.outbound.OutboundOrderSaveRequest;
import com.example.wms.entity.Customer;
import com.example.wms.entity.OutboundOrder;
import com.example.wms.entity.OutboundOrderItem;
import com.example.wms.entity.Product;
import com.example.wms.entity.StorageLocation;
import com.example.wms.entity.SysUser;
import com.example.wms.entity.Warehouse;
import com.example.wms.mapper.CustomerMapper;
import com.example.wms.mapper.OutboundOrderItemMapper;
import com.example.wms.mapper.OutboundOrderMapper;
import com.example.wms.mapper.ProductMapper;
import com.example.wms.mapper.StorageLocationMapper;
import com.example.wms.mapper.SysUserMapper;
import com.example.wms.mapper.WarehouseMapper;
import com.example.wms.security.SecurityUtils;
import com.example.wms.service.InventoryMutationService;
import com.example.wms.service.OutboundOrderService;
import com.example.wms.vo.outbound.OutboundOrderItemVO;
import com.example.wms.vo.outbound.OutboundOrderVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 出库单服务实现。
 * 状态机：DRAFT -> PENDING -> APPROVED / REJECTED；PENDING -> CANCELED。
 * 库存模型：
 * - submit 锁定库存（lockedQuantity += qty），写 LOCK 流水；
 * - approve 扣减实际库存并释放锁定，写 OUTBOUND 流水；
 * - reject / cancel 释放锁定，写 UNLOCK 流水。
 */
@Service
@RequiredArgsConstructor
public class OutboundOrderServiceImpl implements OutboundOrderService {

    private final OutboundOrderMapper outboundOrderMapper;
    private final OutboundOrderItemMapper outboundOrderItemMapper;
    private final WarehouseMapper warehouseMapper;
    private final CustomerMapper customerMapper;
    private final ProductMapper productMapper;
    private final StorageLocationMapper storageLocationMapper;
    private final SysUserMapper sysUserMapper;
    private final InventoryMutationService inventoryMutationService;

    @Override
    public PageResult<OutboundOrderVO> page(OutboundOrderQueryDTO query) {
        Page<OutboundOrder> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<OutboundOrder> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(OutboundOrder::getStatus, query.getStatus());
        }
        if (StringUtils.hasText(query.getType())) {
            wrapper.eq(OutboundOrder::getType, query.getType());
        }
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.like(OutboundOrder::getOrderNo, query.getKeyword());
        }
        if (!SecurityUtils.isAdmin()) {
            wrapper.eq(OutboundOrder::getApplicantId, SecurityUtils.getCurrentUserId());
        }
        wrapper.orderByDesc(OutboundOrder::getCreateTime);

        IPage<OutboundOrder> result = outboundOrderMapper.selectPage(page, wrapper);
        Page<OutboundOrderVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(this::toVO).toList());
        fillNames(voPage.getRecords());
        return PageResult.of(voPage);
    }

    @Override
    public OutboundOrderVO get(Long id) {
        OutboundOrderVO vo = toVO(getOrThrow(id));
        fillNames(List.of(vo));
        vo.setItems(listItems(id));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OutboundOrderVO create(OutboundOrderSaveRequest request) {
        validateRequest(request);
        OutboundOrder order = new OutboundOrder();
        BeanUtils.copyProperties(request, order);
        order.setOrderNo(generateOrderNo());
        order.setStatus("DRAFT");
        order.setApplicantId(SecurityUtils.getCurrentUserId());
        recalcTotals(order, request.getItems());
        outboundOrderMapper.insert(order);
        saveItems(order.getId(), request.getItems());
        return get(order.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OutboundOrderVO update(Long id, OutboundOrderSaveRequest request) {
        OutboundOrder order = getOrThrow(id);
        checkEditable(order);
        checkOwnership(order);
        validateRequest(request);
        BeanUtils.copyProperties(request, order);
        // 驳回后重新编辑：视为重新起草，否则状态仍是 REJECTED 无法再次提交
        if ("REJECTED".equals(order.getStatus())) {
            order.setStatus("DRAFT");
        }
        recalcTotals(order, request.getItems());
        outboundOrderMapper.updateById(order);
        outboundOrderItemMapper.delete(new LambdaQueryWrapper<OutboundOrderItem>()
                .eq(OutboundOrderItem::getOrderId, id));
        saveItems(id, request.getItems());
        return get(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        OutboundOrder order = getOrThrow(id);
        checkEditable(order);
        checkOwnership(order);
        outboundOrderItemMapper.delete(new LambdaQueryWrapper<OutboundOrderItem>()
                .eq(OutboundOrderItem::getOrderId, id));
        outboundOrderMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submit(Long id) {
        OutboundOrder order = getOrThrow(id);
        checkOwnership(order);
        if (!"DRAFT".equals(order.getStatus())) {
            throw new BusinessException(ResultCode.ORDER_STATE_ERROR, "仅草稿状态可提交");
        }
        List<OutboundOrderItem> items = listItemsEntity(id);
        Long operatorId = SecurityUtils.getCurrentUserId();
        // 逐行锁定库存（可用库存不足直接抛异常，事务回滚）
        for (OutboundOrderItem item : items) {
            inventoryMutationService.lock(
                    item.getProductId(), order.getWarehouseId(), item.getLocationId(),
                    item.getQuantity(), order.getOrderNo(), operatorId);
        }
        order.setStatus("PENDING");
        outboundOrderMapper.updateById(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long id) {
        OutboundOrder order = getOrThrow(id);
        if (!"PENDING".equals(order.getStatus())) {
            throw new BusinessException(ResultCode.ORDER_STATE_ERROR, "仅待审核状态可审核");
        }
        List<OutboundOrderItem> items = listItemsEntity(id);
        Long operatorId = SecurityUtils.getCurrentUserId();
        // 逐行扣减实际库存并释放锁定
        for (OutboundOrderItem item : items) {
            inventoryMutationService.outbound(
                    item.getProductId(), order.getWarehouseId(), item.getLocationId(),
                    item.getQuantity(), order.getOrderNo(), operatorId);
        }
        order.setStatus("APPROVED");
        order.setAuditorId(operatorId);
        order.setAuditTime(LocalDateTime.now());
        outboundOrderMapper.updateById(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long id, AuditRequest request) {
        OutboundOrder order = getOrThrow(id);
        if (!"PENDING".equals(order.getStatus())) {
            throw new BusinessException(ResultCode.ORDER_STATE_ERROR, "仅待审核状态可驳回");
        }
        List<OutboundOrderItem> items = listItemsEntity(id);
        Long operatorId = SecurityUtils.getCurrentUserId();
        // 释放锁定
        for (OutboundOrderItem item : items) {
            inventoryMutationService.unlock(
                    item.getProductId(), order.getWarehouseId(), item.getLocationId(),
                    item.getQuantity(), order.getOrderNo(), operatorId);
        }
        order.setStatus("REJECTED");
        order.setAuditorId(operatorId);
        order.setAuditTime(LocalDateTime.now());
        order.setAuditRemark(request != null ? request.getAuditRemark() : null);
        outboundOrderMapper.updateById(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id) {
        OutboundOrder order = getOrThrow(id);
        checkOwnership(order);
        if (!"PENDING".equals(order.getStatus())) {
            throw new BusinessException(ResultCode.ORDER_STATE_ERROR, "仅待审核状态可作废");
        }
        List<OutboundOrderItem> items = listItemsEntity(id);
        Long operatorId = SecurityUtils.getCurrentUserId();
        for (OutboundOrderItem item : items) {
            inventoryMutationService.unlock(
                    item.getProductId(), order.getWarehouseId(), item.getLocationId(),
                    item.getQuantity(), order.getOrderNo(), operatorId);
        }
        order.setStatus("CANCELED");
        outboundOrderMapper.updateById(order);
    }

    // ==================== 私有方法 ====================

    private OutboundOrder getOrThrow(Long id) {
        OutboundOrder order = outboundOrderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "出库单不存在");
        }
        return order;
    }

    private void checkEditable(OutboundOrder order) {
        if (!"DRAFT".equals(order.getStatus()) && !"REJECTED".equals(order.getStatus())) {
            throw new BusinessException(ResultCode.ORDER_STATE_ERROR, "当前状态不允许编辑");
        }
    }

    private void checkOwnership(OutboundOrder order) {
        if (!SecurityUtils.isAdmin()
                && !order.getApplicantId().equals(SecurityUtils.getCurrentUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权操作他人单据");
        }
    }

    private void validateRequest(OutboundOrderSaveRequest request) {
        if (warehouseMapper.selectById(request.getWarehouseId()) == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "仓库不存在");
        }
        if ("SALE".equals(request.getType()) && request.getCustomerId() != null) {
            if (customerMapper.selectById(request.getCustomerId()) == null) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "客户不存在");
            }
        }
        for (OutboundOrderItemDTO item : request.getItems()) {
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "出库数量必须大于 0");
            }
            if (productMapper.selectById(item.getProductId()) == null) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "商品不存在");
            }
            StorageLocation loc = storageLocationMapper.selectById(item.getLocationId());
            if (loc == null) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "库位不存在");
            }
            if (!loc.getWarehouseId().equals(request.getWarehouseId())) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "库位不属于所选仓库");
            }
        }
    }

    private void recalcTotals(OutboundOrder order, List<OutboundOrderItemDTO> items) {
        int totalQty = items.stream().mapToInt(OutboundOrderItemDTO::getQuantity).sum();
        BigDecimal totalAmt = items.stream()
                .map(i -> (i.getPrice() == null ? BigDecimal.ZERO : i.getPrice())
                        .multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setTotalQuantity(totalQty);
        order.setTotalAmount(totalAmt);
    }

    private void saveItems(Long orderId, List<OutboundOrderItemDTO> items) {
        for (OutboundOrderItemDTO dto : items) {
            OutboundOrderItem item = new OutboundOrderItem();
            BeanUtils.copyProperties(dto, item);
            item.setOrderId(orderId);
            if (item.getPrice() == null) {
                item.setPrice(BigDecimal.ZERO);
            }
            item.setAmount(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            outboundOrderItemMapper.insert(item);
        }
    }

    private List<OutboundOrderItem> listItemsEntity(Long orderId) {
        return outboundOrderItemMapper.selectList(new LambdaQueryWrapper<OutboundOrderItem>()
                .eq(OutboundOrderItem::getOrderId, orderId));
    }

    private List<OutboundOrderItemVO> listItems(Long orderId) {
        List<OutboundOrderItem> items = listItemsEntity(orderId);
        List<OutboundOrderItemVO> vos = items.stream().map(i -> {
            OutboundOrderItemVO vo = new OutboundOrderItemVO();
            BeanUtils.copyProperties(i, vo);
            return vo;
        }).toList();
        fillItemNames(vos);
        return vos;
    }

    private OutboundOrderVO toVO(OutboundOrder order) {
        OutboundOrderVO vo = new OutboundOrderVO();
        BeanUtils.copyProperties(order, vo);
        return vo;
    }

    private void fillNames(List<OutboundOrderVO> vos) {
        if (vos == null || vos.isEmpty()) {
            return;
        }
        Set<Long> whIds = vos.stream().map(OutboundOrderVO::getWarehouseId).collect(Collectors.toSet());
        Set<Long> cusIds = vos.stream().map(OutboundOrderVO::getCustomerId).filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        Set<Long> userIds = vos.stream().map(OutboundOrderVO::getApplicantId).filter(java.util.Objects::nonNull).collect(Collectors.toSet());

        Map<Long, String> whMap = warehouseMapper.selectBatchIds(whIds).stream()
                .collect(Collectors.toMap(Warehouse::getId, Warehouse::getName));
        Map<Long, String> cusMap = cusIds.isEmpty() ? Map.of()
                : customerMapper.selectBatchIds(cusIds).stream().collect(Collectors.toMap(Customer::getId, Customer::getName));
        Map<Long, String> userMap = userIds.isEmpty() ? Map.of()
                : sysUserMapper.selectBatchIds(userIds).stream().collect(Collectors.toMap(SysUser::getId, SysUser::getRealName));

        vos.forEach(vo -> {
            vo.setWarehouseName(whMap.get(vo.getWarehouseId()));
            vo.setCustomerName(cusMap.get(vo.getCustomerId()));
            vo.setApplicantName(userMap.get(vo.getApplicantId()));
        });
    }

    private void fillItemNames(List<OutboundOrderItemVO> vos) {
        if (vos == null || vos.isEmpty()) {
            return;
        }
        Set<Long> productIds = vos.stream().map(OutboundOrderItemVO::getProductId).collect(Collectors.toSet());
        Set<Long> locIds = vos.stream().map(OutboundOrderItemVO::getLocationId).collect(Collectors.toSet());
        Map<Long, Product> pMap = productMapper.selectBatchIds(productIds).stream()
                .collect(Collectors.toMap(Product::getId, p -> p));
        Map<Long, String> locMap = storageLocationMapper.selectBatchIds(locIds).stream()
                .collect(Collectors.toMap(StorageLocation::getId, StorageLocation::getCode));
        vos.forEach(vo -> {
            Product p = pMap.get(vo.getProductId());
            if (p != null) {
                vo.setProductName(p.getName());
                vo.setSku(p.getSku());
            }
            vo.setLocationCode(locMap.get(vo.getLocationId()));
        });
    }

    private String generateOrderNo() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int count = outboundOrderMapper.countTodayOrders(dateStr);
        return "OUT" + dateStr + String.format("%03d", count + 1);
    }
}
