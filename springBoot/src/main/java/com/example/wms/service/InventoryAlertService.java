package com.example.wms.service;

import com.example.wms.common.PageResult;
import com.example.wms.dto.alert.AlertHandleRequest;
import com.example.wms.dto.alert.AlertQueryDTO;
import com.example.wms.vo.alert.AlertVO;

/** 库存预警记录服务 */
public interface InventoryAlertService {

    PageResult<AlertVO> page(AlertQueryDTO query);

    AlertVO get(Long id);

    /** 处理预警（标记已处理 / 已忽略），仅管理员 */
    void handle(Long id, AlertHandleRequest request);

    /** 批量处理未处理预警 */
    int batchHandle(AlertHandleRequest request);
}
