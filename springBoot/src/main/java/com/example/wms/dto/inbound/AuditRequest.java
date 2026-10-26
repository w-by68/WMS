package com.example.wms.dto.inbound;

import lombok.Data;

/** 审核请求（驳回时填写原因） */
@Data
public class AuditRequest {

    /** 驳回原因（驳回时必填） */
    private String auditRemark;
}
