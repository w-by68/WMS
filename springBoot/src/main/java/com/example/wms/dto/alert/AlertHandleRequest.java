package com.example.wms.dto.alert;

import lombok.Data;

/** 预警处理请求（标记已处理 / 已忽略） */
@Data
public class AlertHandleRequest {

    /** 处理动作：RESOLVED-已处理 IGNORED-已忽略 */
    private String status;

    /** 处理备注 */
    private String handleRemark;
}
