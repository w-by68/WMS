package com.example.wms.service;

import com.example.wms.common.PageResult;
import com.example.wms.dto.inbound.AuditRequest;
import com.example.wms.dto.inbound.InboundOrderQueryDTO;
import com.example.wms.dto.inbound.InboundOrderSaveRequest;
import com.example.wms.vo.inbound.InboundOrderVO;

/** 入库单服务 */
public interface InboundOrderService {

    PageResult<InboundOrderVO> page(InboundOrderQueryDTO query);

    InboundOrderVO get(Long id);

    /** 新建入库单（DRAFT） */
    InboundOrderVO create(InboundOrderSaveRequest request);

    /** 修改入库单（仅 DRAFT/REJECTED） */
    InboundOrderVO update(Long id, InboundOrderSaveRequest request);

    /** 删除入库单（仅 DRAFT/REJECTED） */
    void delete(Long id);

    /** 提交审核：DRAFT -> PENDING */
    void submit(Long id);

    /** 审核通过：PENDING -> APPROVED，增加库存 */
    void approve(Long id);

    /** 驳回：PENDING -> REJECTED */
    void reject(Long id, AuditRequest request);

    /** 作废：PENDING -> CANCELED */
    void cancel(Long id);
}
