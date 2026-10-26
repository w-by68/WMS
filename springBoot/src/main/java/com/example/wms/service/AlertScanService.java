package com.example.wms.service;

/**
 * 库存预警扫描服务（"智能"核心）。
 * 遍历所有启用规则，对每条库存记录判定缺货 / 积压 / 呆滞，
 * 生成或更新预警记录（同商品+仓库+类型的未处理预警只更新不重复插入）。
 */
public interface AlertScanService {

    /**
     * 执行一次全量扫描。由定时任务或手动触发调用。
     *
     * @return 本次新增 + 更新的预警记录数
     */
    int scan();
}
