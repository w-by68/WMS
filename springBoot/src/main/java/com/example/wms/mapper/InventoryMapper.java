package com.example.wms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.wms.dto.inventory.InventoryQueryDTO;
import com.example.wms.entity.Inventory;
import com.example.wms.vo.inventory.InventoryVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/** 库存表 Mapper */
@Mapper
public interface InventoryMapper extends BaseMapper<Inventory> {

    /**
     * 库存分页（已带商品/仓库/库位名称与安全库存）。
     * 安全库存取自 inventory_alert_rule：优先匹配「商品 + 具体仓库」的规则，
     * 其次退到 warehouse_id 为 NULL 的通用规则，都没有则为 0。
     * 之所以用自定义 JOIN SQL 而不是 LambdaQueryWrapper：onlyShortage（可用库存 < 安全库存）
     * 必须在数据库层过滤，否则会先分页再过滤导致每页条数错乱。
     */
    @Select("<script>"
            + "SELECT i.id AS id, i.product_id AS productId, p.name AS productName, p.sku AS sku, p.spec AS spec, "
            + "i.warehouse_id AS warehouseId, w.name AS warehouseName, "
            + "i.location_id AS locationId, l.code AS locationCode, "
            + "i.quantity AS quantity, i.locked_quantity AS lockedQuantity, "
            + "(i.quantity - i.locked_quantity) AS availableQuantity, "
            + "i.version AS version, i.last_inbound_time AS lastInboundTime, i.last_outbound_time AS lastOutboundTime, "
            + "IFNULL(r.safety_stock, IFNULL(rg.safety_stock, 0)) AS safetyStock "
            + "FROM inventory i "
            + "JOIN product p ON p.id = i.product_id "
            + "LEFT JOIN warehouse w ON w.id = i.warehouse_id "
            + "LEFT JOIN storage_location l ON l.id = i.location_id "
            + "LEFT JOIN inventory_alert_rule r ON r.product_id = i.product_id "
            + "  AND r.warehouse_id = i.warehouse_id AND r.enabled = 1 "
            + "LEFT JOIN inventory_alert_rule rg ON rg.product_id = i.product_id "
            + "  AND rg.warehouse_id IS NULL AND rg.enabled = 1 "
            + "<where>"
            + "<if test=\"q.productId != null\"> AND i.product_id = #{q.productId}</if>"
            + "<if test=\"q.warehouseId != null\"> AND i.warehouse_id = #{q.warehouseId}</if>"
            + "<if test=\"q.locationId != null\"> AND i.location_id = #{q.locationId}</if>"
            + "<if test=\"q.keyword != null and q.keyword != ''\">"
            + " AND (p.sku LIKE CONCAT('%', #{q.keyword}, '%') OR p.name LIKE CONCAT('%', #{q.keyword}, '%'))</if>"
            + "<if test=\"q.onlyShortage != null and q.onlyShortage == true\">"
            + " AND (i.quantity - i.locked_quantity) &lt; IFNULL(r.safety_stock, IFNULL(rg.safety_stock, 0))</if>"
            + "</where>"
            + " ORDER BY i.create_time DESC"
            + "</script>")
    IPage<InventoryVO> selectVoPage(IPage<InventoryVO> page, @Param("q") InventoryQueryDTO query);

    /**
     * 按商品汇总库存（跨仓库）：返回 productId / quantity 合计 / lockedQuantity 合计。
     * 用于"按商品看总库存"的看板场景。
     */
    @Select("SELECT product_id AS productId, " +
            "SUM(quantity) AS totalQuantity, " +
            "SUM(locked_quantity) AS totalLockedQuantity " +
            "FROM inventory " +
            "GROUP BY product_id " +
            "ORDER BY product_id")
    List<Map<String, Object>> summaryByProduct();

    /**
     * 按仓库汇总库存：返回 warehouseId / totalQuantity / totalValue。
     * totalValue = SUM(inventory.quantity * product.cost_price)，用于看板库存总值。
     */
    @Select("SELECT i.warehouse_id AS warehouseId, " +
            "SUM(i.quantity) AS totalQuantity, " +
            "SUM(i.quantity * IFNULL(p.cost_price, 0)) AS totalValue " +
            "FROM inventory i " +
            "LEFT JOIN product p ON i.product_id = p.id " +
            "GROUP BY i.warehouse_id")
    List<Map<String, Object>> summaryByWarehouse();

    /**
     * 按商品分类汇总账面库存（看板饼图）：返回 categoryName / quantity。
     * 商品未挂分类时归为"未分类"。仅统计未删除商品。
     */
    @Select("SELECT IFNULL(pc.name, '未分类') AS categoryName, " +
            "SUM(i.quantity) AS quantity " +
            "FROM inventory i " +
            "JOIN product p ON p.id = i.product_id " +
            "LEFT JOIN product_category pc ON pc.id = p.category_id " +
            "WHERE p.deleted = 0 " +
            "GROUP BY pc.id, pc.name " +
            "ORDER BY quantity DESC")
    List<Map<String, Object>> categoryStock();
}
