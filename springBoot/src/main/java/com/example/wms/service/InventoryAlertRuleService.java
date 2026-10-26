package com.example.wms.service;

import com.example.wms.common.PageResult;
import com.example.wms.dto.alertrule.AlertRuleQueryDTO;
import com.example.wms.dto.alertrule.AlertRuleSaveRequest;
import com.example.wms.vo.alertrule.AlertRuleVO;

/** 库存预警规则服务（仅管理员可维护） */
public interface InventoryAlertRuleService {

    PageResult<AlertRuleVO> page(AlertRuleQueryDTO query);

    AlertRuleVO get(Long id);

    AlertRuleVO create(AlertRuleSaveRequest request);

    AlertRuleVO update(Long id, AlertRuleSaveRequest request);

    void delete(Long id);

    /** 启用/停用规则 */
    void toggleEnabled(Long id, Integer enabled);
}
