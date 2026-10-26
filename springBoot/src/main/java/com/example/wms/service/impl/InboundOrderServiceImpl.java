package com.example.wms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.wms.common.BusinessException;
import com.example.wms.common.PageResult;
import com.example.wms.common.ResultCode;
import com.example.wms.dto.inbound.AuditRequest;
import com.example.wms.dto.inbound.InboundOrderItemDTO;
import com.example.wms.dto.inbound.InboundOrderQueryDTO;
import com.example.wms.dto.inbound.InboundOrderSaveRequest;
import com.example.wms.entity.InboundOrder;
import com.example.wms.entity.InboundOrderItem;
import com.example.wms.entity.Product;
import com.example.wms.entity.StorageLocation;
import com.example.wms.entity.Supplier;
import com.example.wms.entity.SysUser;
import com.example.wms.entity.Warehouse;
import com.example.wms.mapper.InboundOrderItemMapper;
import com.example.wms.mapper.InboundOrderMapper;
import com.example.wms.mapper.ProductMapper;
import com.example.wms.mapper.StorageLocationMapper;
import com.example.wms.mapper.SupplierMapper;
import com.example.wms.mapper.SysUserMapper;
import com.example.wms.mapper.WarehouseMapper;
import com.example.wms.security.SecurityUtils;
import com.example.wms.service.InboundOrderService;
import com.example.wms.service.InventoryMutationService;
import com.example.wms.vo.inbound.InboundOrderItemVO;
import com.example.wms.vo.inbound.InboundOrderVO;
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
 * 入库单服务实现。
 * 状态机：DRAFT -> PENDING -> APPROVED / REJECTED；PENDING -> CANCELED。
 * 仅 DRAFT / REJECTED 可编辑/删除；APPROVED 时增加库存。
 */
@Service
@RequiredArgsConstructor
public class InboundOrderServiceImpl implements InboundOrderService {

    private final InboundOrderMapper inboundOrderMapper;
    private final InboundOrderItemMapper inboundOrderItemMapper;
    private final WarehouseMapper warehouseMapper;
    private final SupplierMapper supplierMapper;
    private final ProductMapper productMapper;
    private final StorageLocationMapper storageLocationMapper;
    private final SysUserMapper sysUserMapper;
    private final InventoryMutationService inventoryMutationService;

    @Override
    public PageResult<InboundOrderVO> page(InboundOrderQueryDTO query) {
        Page<InboundOrder> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<InboundOrder> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(InboundOrder::getStatus, query.getStatus());
        }
        if (StringUtils.hasText(query.getType())) {
            wrapper.eq(InboundOrder::getType, query.getType());
        }
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.like(InboundOrder::getOrderNo, query.getKeyword());
        }
        // 数据范围：USER 只看自己的单
        if (!SecurityUtils.isAdmin()) {
            wrapper.eq(InboundOrder::getApplicantId, SecurityUtils.getCurrentUserId());
        }
        wrapper.orderByDesc(InboundOrder::getCreateTime);

        IPage<InboundOrder> result = inboundOrderMapper.selectPage(page, wrapper);
        Page<InboundOrderVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(this::toVO).toList());
        fillNames(voPage.getRecords());
        return PageResult.of(voPage);
    }

    @Override
    public InboundOrderVO get(Long id) {
        InboundOrderVO vo = toVO(getOrThrow(id));
        fillNames(List.of(vo));
        vo.setItems(listItems(id));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InboundOrderVO create(InboundOrderSaveRequest request) {
        validateRequest(request);
        InboundOrder order = new InboundOrder();
        BeanUtils.copyProperties(request, order);
        order.setOrderNo(generateOrderNo());
        order.setStatus("DRAFT");
        order.setApplicantId(SecurityUtils.getCurrentUserId());
        recalcTotals(order, request.getItems());
        inboundOrderMapper.insert(order);
        saveItems(order.getId(), request.getItems());
        return get(order.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InboundOrderVO update(Long id, InboundOrderSaveRequest request) {
        InboundOrder order = getOrThrow(id);
        checkEditable(order);
        checkOwnership(order);
        validateRequest(request);
        BeanUtils.copyProperties(request, order);
        // 驳回后重新编辑：视为重新起草，否则状态仍是 REJECTED 无法再次提交
        if ("REJECTED".equals(order.getStatus())) {
            order.setStatus("DRAFT");
        }
        recalcTotals(order, request.getItems());
        inboundOrderMapper.updateById(order);
        // 明细整体替换
        inboundOrderItemMapper.delete(new LambdaQueryWrapper<InboundOrderItem>()
                .eq(InboundOrderItem::getOrderId, id));
        saveItems(id, request.getItems());
        return get(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        InboundOrder order = getOrThrow(id);
        checkEditable(order);
        checkOwnership(order);
        inboundOrderItemMapper.delete(new LambdaQueryWrapper<InboundOrderItem>()
                .eq(InboundOrderItem::getOrderId, id));
        inboundOrderMapper.deleteById(id);
    }

    @Override
    public void submit(Long id) {
        InboundOrder order = getOrThrow(id);
        checkOwnership(order);
        if (!"DRAFT".equals(order.getStatus())) {
            throw new BusinessException(ResultCode.ORDER_STATE_ERROR, "仅草稿状态可提交");
        }
        order.setStatus("PENDING");
        inboundOrderMapper.updateById(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long id) {
        InboundOrder order = getOrThrow(id);
        if (!"PENDING".equals(order.getStatus())) {
            throw new BusinessException(ResultCode.ORDER_STATE_ERROR, "仅待审核状态可审核");
        }
        List<InboundOrderItem> items = listItemsEntity(id);
        Long operatorId = SecurityUtils.getCurrentUserId();
        // 逐行入库
        for (InboundOrderItem item : items) {
            inventoryMutationService.inbound(
                    item.getProductId(), order.getWarehouseId(), item.getLocationId(),
                    item.getQuantity(), order.getOrderNo(), operatorId);
        }
        order.setStatus("APPROVED");
        order.setAuditorId(operatorId);
        order.setAuditTime(LocalDateTime.now());
        inboundOrderMapper.updateById(order);
    }

    @Override
    public void reject(Long id, AuditRequest request) {
        InboundOrder order = getOrThrow(id);
        if (!"PENDING".equals(order.getStatus())) {
            throw new BusinessException(ResultCode.ORDER_STATE_ERROR, "仅待审核状态可驳回");
        }
        order.setStatus("REJECTED");
        order.setAuditorId(SecurityUtils.getCurrentUserId());
        order.setAuditTime(LocalDateTime.now());
        order.setAuditRemark(request != null ? request.getAuditRemark() : null);
        inboundOrderMapper.updateById(order);
    }

    @Override
    public void cancel(Long id) {
        InboundOrder order = getOrThrow(id);
        checkOwnership(order);
        if (!"PENDING".equals(order.getStatus())) {
            throw new BusinessException(ResultCode.ORDER_STATE_ERROR, "仅待审核状态可作废");
        }
        order.setStatus("CANCELED");
        inboundOrderMapper.updateById(order);
    }

    // ==================== 私有方法 ====================

    private InboundOrder getOrThrow(Long id) {
        InboundOrder order = inboundOrderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "入库单不存在");
        }
        return order;
    }

    /** 仅 DRAFT / REJECTED 可编辑 */
    private void checkEditable(InboundOrder order) {
        if (!"DRAFT".equals(order.getStatus()) && !"REJECTED".equals(order.getStatus())) {
            throw new BusinessException(ResultCode.ORDER_STATE_ERROR, "当前状态不允许编辑");
        }
    }

    /** 普通用户只能操作自己的单 */
    private void checkOwnership(InboundOrder order) {
        if (!SecurityUtils.isAdmin()
                && !order.getApplicantId().equals(SecurityUtils.getCurrentUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权操作他人单据");
        }
    }

    /** 校验请求：仓库/供应商/商品/库位存在，库位属于仓库 */
    private void validateRequest(InboundOrderSaveRequest request) {
        if (warehouseMapper.selectById(request.getWarehouseId()) == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "仓库不存在");
        }
        if ("PURCHASE".equals(request.getType()) && request.getSupplierId() != null) {
            if (supplierMapper.selectById(request.getSupplierId()) == null) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "供应商不存在");
            }
        }
        for (InboundOrderItemDTO item : request.getItems()) {
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "入库数量必须大于 0");
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

    /** 重算合计数量与金额 */
    private void recalcTotals(InboundOrder order, List<InboundOrderItemDTO> items) {
        int totalQty = items.stream().mapToInt(InboundOrderItemDTO::getQuantity).sum();
        BigDecimal totalAmt = items.stream()
                .map(i -> (i.getPrice() == null ? BigDecimal.ZERO : i.getPrice())
                        .multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setTotalQuantity(totalQty);
        order.setTotalAmount(totalAmt);
    }

    private void saveItems(Long orderId, List<InboundOrderItemDTO> items) {
        for (InboundOrderItemDTO dto : items) {
            InboundOrderItem item = new InboundOrderItem();
            BeanUtils.copyProperties(dto, item);
            item.setOrderId(orderId);
            if (item.getPrice() == null) {
                item.setPrice(BigDecimal.ZERO);
            }
            item.setAmount(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            inboundOrderItemMapper.insert(item);
        }
    }

    private List<InboundOrderItem> listItemsEntity(Long orderId) {
        return inboundOrderItemMapper.selectList(new LambdaQueryWrapper<InboundOrderItem>()
                .eq(InboundOrderItem::getOrderId, orderId));
    }

    private List<InboundOrderItemVO> listItems(Long orderId) {
        List<InboundOrderItem> items = listItemsEntity(orderId);
        List<InboundOrderItemVO> vos = items.stream().map(i -> {
            InboundOrderItemVO vo = new InboundOrderItemVO();
            BeanUtils.copyProperties(i, vo);
            return vo;
        }).toList();
        fillItemNames(vos);
        return vos;
    }

    private InboundOrderVO toVO(InboundOrder order) {
        InboundOrderVO vo = new InboundOrderVO();
        BeanUtils.copyProperties(order, vo);
        return vo;
    }

    private void fillNames(List<InboundOrderVO> vos) {
        if (vos == null || vos.isEmpty()) {
            return;
        }
        Set<Long> whIds = vos.stream().map(InboundOrderVO::getWarehouseId).collect(Collectors.toSet());
        Set<Long> supIds = vos.stream().map(InboundOrderVO::getSupplierId).filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        Set<Long> userIds = vos.stream().map(InboundOrderVO::getApplicantId).filter(java.util.Objects::nonNull).collect(Collectors.toSet());

        Map<Long, String> whMap = warehouseMapper.selectBatchIds(whIds).stream()
                .collect(Collectors.toMap(Warehouse::getId, Warehouse::getName));
        Map<Long, String> supMap = supIds.isEmpty() ? Map.of()
                : supplierMapper.selectBatchIds(supIds).stream().collect(Collectors.toMap(Supplier::getId, Supplier::getName));
        Map<Long, String> userMap = userIds.isEmpty() ? Map.of()
                : sysUserMapper.selectBatchIds(userIds).stream().collect(Collectors.toMap(SysUser::getId, SysUser::getRealName));

        vos.forEach(vo -> {
            vo.setWarehouseName(whMap.get(vo.getWarehouseId()));
            vo.setSupplierName(supMap.get(vo.getSupplierId()));
            vo.setApplicantName(userMap.get(vo.getApplicantId()));
        });
    }

    private void fillItemNames(List<InboundOrderItemVO> vos) {
        if (vos == null || vos.isEmpty()) {
            return;
        }
        Set<Long> productIds = vos.stream().map(InboundOrderItemVO::getProductId).collect(Collectors.toSet());
        Set<Long> locIds = vos.stream().map(InboundOrderItemVO::getLocationId).collect(Collectors.toSet());
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
        int count = inboundOrderMapper.countTodayOrders(dateStr);
        return "IN" + dateStr + String.format("%03d", count + 1);
    }
}
