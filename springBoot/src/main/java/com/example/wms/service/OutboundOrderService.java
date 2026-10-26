package com.example.wms.service;

import com.example.wms.common.PageResult;
import com.example.wms.dto.inbound.AuditRequest;
import com.example.wms.dto.outbound.OutboundOrderQueryDTO;
import com.example.wms.dto.outbound.OutboundOrderSaveRequest;
import com.example.wms.vo.outbound.OutboundOrderVO;

/** 出库单服务 */
public interface OutboundOrderService {

    PageResult<OutboundOrderVO> page(OutboundOrderQueryDTO query);

    OutboundOrderVO get(Long id);

    /** 新建出库单（DRAFT） */
    OutboundOrderVO create(OutboundOrderSaveRequest request);

    /** 修改出库单（仅 DRAFT/REJECTED） */
    OutboundOrderVO update(Long id, OutboundOrderSaveRequest request);

    /** 删除出库单（仅 DRAFT/REJECTED） */
    void delete(Long id);

    /** 提交审核：DRAFT -> PENDING，同时锁定库存 */
    void submit(Long id);

    /** 审核通过：PENDING -> APPROVED，扣减实际库存并释放锁定 */
    void approve(Long id);

    /** 驳回：PENDING -> REJECTED，释放锁定库存 */
    void reject(Long id, AuditRequest request);

    /** 作废：PENDING -> CANCELED，释放锁定库存 */
    void cancel(Long id);
}
