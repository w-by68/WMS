package com.example.wms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.wms.entity.Inventory;
import com.example.wms.entity.InventoryAlert;
import com.example.wms.entity.InventoryAlertRule;
import com.example.wms.mapper.InventoryAlertMapper;
import com.example.wms.mapper.InventoryAlertRuleMapper;
import com.example.wms.mapper.InventoryMapper;
import com.example.wms.service.AlertScanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 预警扫描服务实现。
 * <p>
 * 匹配优先级：具体仓库规则 > 全仓库通用规则（warehouseId IS NULL）。
 * 三类预警：
 * - SHORTAGE：可用库存 < 安全库存下限
 * - OVERSTOCK：可用库存 > 库存上限（上限 > 0 时才判定）
 * - DEAD_STOCK：距最近出库时间（无则取创建时间）超过呆滞天数
 * <p>
 * 去重规则：同一 productId + warehouseId + alertType 若存在 UNRESOLVED 记录，
 * 只更新数量/级别/建议量；否则插入新记录。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertScanServiceImpl implements AlertScanService {

    private final InventoryMapper inventoryMapper;
    private final InventoryAlertRuleMapper alertRuleMapper;
    private final InventoryAlertMapper alertMapper;

    /** 定时全量扫描：每 30 分钟执行一次 */
    @Scheduled(cron = "0 */30 * * * ?")
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int scan() {
        log.info("[预警扫描] 开始执行全量扫描");
        long start = System.currentTimeMillis();

        // 1. 加载所有启用规则，按 productId 分组
        List<InventoryAlertRule> rules = alertRuleMapper.selectList(
                new LambdaQueryWrapper<InventoryAlertRule>().eq(InventoryAlertRule::getEnabled, 1));
        if (rules.isEmpty()) {
            log.info("[预警扫描] 无启用规则，跳过");
            return 0;
        }
        // productId -> (warehouseId -> rule)，warehouseId=null 的作为通用规则
        Map<Long, Map<Long, InventoryAlertRule>> ruleIndex = new HashMap<>();
        for (InventoryAlertRule rule : rules) {
            ruleIndex.computeIfAbsent(rule.getProductId(), k -> new HashMap<>())
                    .put(rule.getWarehouseId(), rule);
        }

        // 2. 加载所有库存记录
        List<Inventory> inventories = inventoryMapper.selectList(null);
        int changed = 0;

        for (Inventory inv : inventories) {
            Map<Long, InventoryAlertRule> whRules = ruleIndex.get(inv.getProductId());
            if (whRules == null || whRules.isEmpty()) {
                continue;
            }
            // 优先匹配具体仓库规则，没有则取通用规则（key=null）
            InventoryAlertRule rule = whRules.get(inv.getWarehouseId());
            if (rule == null) {
                rule = whRules.get(null);
            }
            if (rule == null) {
                continue;
            }

            int available = (inv.getQuantity() == null ? 0 : inv.getQuantity())
                    - (inv.getLockedQuantity() == null ? 0 : inv.getLockedQuantity());

            // 缺货
            if (available < rule.getSafetyStock()) {
                changed += upsertAlert(inv, rule, "SHORTAGE", available,
                        rule.getSafetyStock(),
                        rule.getSafetyStock() - available,
                        available == 0 || available < rule.getSafetyStock() * 0.5 ? "CRITICAL" : "WARNING");
            }
            // 积压（上限 > 0 才判定）
            if (rule.getMaxStock() != null && rule.getMaxStock() > 0 && available > rule.getMaxStock()) {
                changed += upsertAlert(inv, rule, "OVERSTOCK", available,
                        rule.getMaxStock(),
                        available - rule.getMaxStock(),
                        "WARNING");
            }
            // 呆滞
            int deadDays = rule.getDeadStockDays() == null ? 90 : rule.getDeadStockDays();
            LocalDateTime refTime = inv.getLastOutboundTime() != null
                    ? inv.getLastOutboundTime() : inv.getCreateTime();
            long daysSinceOut = ChronoUnit.DAYS.between(refTime.toLocalDate(), LocalDate.now());
            if (refTime != null && daysSinceOut > deadDays) {
                changed += upsertAlert(inv, rule, "DEAD_STOCK", available,
                        (int) daysSinceOut,
                        available,
                        "INFO");
            }
        }

        log.info("[预警扫描] 完成，处理 {} 条预警，耗时 {} ms", changed, System.currentTimeMillis() - start);
        return changed;
    }

    /**
     * 生成或更新预警记录。
     * 同 productId + warehouseId + alertType 的 UNRESOLVED 记录只更新，否则插入。
     */
    private int upsertAlert(Inventory inv, InventoryAlertRule rule, String alertType,
                            int currentQuantity, int thresholdValue, int suggestQuantity, String level) {
        InventoryAlert existing = alertMapper.selectOne(new LambdaQueryWrapper<InventoryAlert>()
                .eq(InventoryAlert::getProductId, inv.getProductId())
                .eq(InventoryAlert::getWarehouseId, inv.getWarehouseId())
                .eq(InventoryAlert::getAlertType, alertType)
                .eq(InventoryAlert::getStatus, "UNRESOLVED")
                .last("LIMIT 1"));

        if (existing != null) {
            existing.setCurrentQuantity(currentQuantity);
            existing.setThresholdValue(thresholdValue);
            existing.setSuggestQuantity(suggestQuantity);
            existing.setLevel(level);
            alertMapper.updateById(existing);
            return 1;
        }

        InventoryAlert alert = new InventoryAlert();
        alert.setAlertNo(generateAlertNo());
        alert.setRuleId(rule.getId());
        alert.setProductId(inv.getProductId());
        alert.setWarehouseId(inv.getWarehouseId());
        alert.setAlertType(alertType);
        alert.setLevel(level);
        alert.setCurrentQuantity(currentQuantity);
        alert.setThresholdValue(thresholdValue);
        alert.setSuggestQuantity(suggestQuantity);
        alert.setStatus("UNRESOLVED");
        alertMapper.insert(alert);
        return 1;
    }

    /** 预警单号：AL + yyyyMMdd + 3 位当日序号 */
    private String generateAlertNo() {
        String dateStr = LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        int count = alertMapper.countTodayAlerts(dateStr);
        return "AL" + dateStr + String.format("%03d", count + 1);
    }
}
