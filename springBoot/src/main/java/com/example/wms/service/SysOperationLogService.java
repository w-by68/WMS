package com.example.wms.service;

import com.example.wms.common.PageResult;
import com.example.wms.dto.log.OperationLogQueryDTO;
import com.example.wms.entity.SysOperationLog;
import com.example.wms.vo.log.OperationLogVO;

/**
 * 操作日志服务：异步写入，避免拖慢业务主流程。
 */
public interface SysOperationLogService {

    /** 异步保存操作日志（@Async 在实现类方法上） */
    void saveAsync(SysOperationLog log);

    /** 管理员审计查询：按操作人/模块/结果/时间范围分页 */
    PageResult<OperationLogVO> page(OperationLogQueryDTO query);
}
