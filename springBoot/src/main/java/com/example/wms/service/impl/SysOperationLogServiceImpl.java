package com.example.wms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.wms.common.PageResult;
import com.example.wms.dto.log.OperationLogQueryDTO;
import com.example.wms.entity.SysOperationLog;
import com.example.wms.mapper.SysOperationLogMapper;
import com.example.wms.service.SysOperationLogService;
import com.example.wms.vo.log.OperationLogVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 操作日志异步落库实现。
 * 为什么异步：日志写入与业务主流程无关，异步化可降低接口 RT，
 * 即便日志写失败也不影响业务结果（主事务已提交）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysOperationLogServiceImpl implements SysOperationLogService {

    private final SysOperationLogMapper sysOperationLogMapper;

    @Override
    @Async
    public void saveAsync(SysOperationLog operationLog) {
        try {
            sysOperationLogMapper.insert(operationLog);
        } catch (Exception e) {
            // 日志写失败只记 error，不向上抛，避免影响调用方
            log.error("操作日志写入失败: {}", e.getMessage(), e);
        }
    }

    @Override
    public PageResult<OperationLogVO> page(OperationLogQueryDTO query) {
        Page<SysOperationLog> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<SysOperationLog> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getUsername())) {
            wrapper.like(SysOperationLog::getUsername, query.getUsername());
        }
        if (StringUtils.hasText(query.getModule())) {
            wrapper.like(SysOperationLog::getModule, query.getModule());
        }
        if (query.getSuccess() != null) {
            wrapper.eq(SysOperationLog::getSuccess, query.getSuccess());
        }
        if (query.getStartTime() != null) {
            wrapper.ge(SysOperationLog::getCreateTime, query.getStartTime());
        }
        if (query.getEndTime() != null) {
            wrapper.le(SysOperationLog::getCreateTime, query.getEndTime());
        }
        wrapper.orderByDesc(SysOperationLog::getCreateTime);

        IPage<SysOperationLog> result = sysOperationLogMapper.selectPage(page, wrapper);
        Page<OperationLogVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(this::toVO).toList());
        return PageResult.of(voPage);
    }

    private OperationLogVO toVO(SysOperationLog log) {
        OperationLogVO vo = new OperationLogVO();
        vo.setId(log.getId());
        vo.setUserId(log.getUserId());
        vo.setUsername(log.getUsername());
        vo.setModule(log.getModule());
        vo.setOperation(log.getOperation());
        vo.setMethod(log.getMethod());
        vo.setRequestUrl(log.getRequestUrl());
        vo.setIp(log.getIp());
        vo.setCostMs(log.getCostMs());
        vo.setSuccess(log.getSuccess());
        vo.setErrorMsg(log.getErrorMsg());
        vo.setCreateTime(log.getCreateTime());
        return vo;
    }
}
