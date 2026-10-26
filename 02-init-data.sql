-- =============================================================
-- 初始化数据：管理员账号 + 演示业务数据
-- 注意：密码均为 BCrypt("123456")，直接可用
-- =============================================================

USE `wms_db`;

-- 1. 用户（系统不开放注册，账号由管理员创建）
--    admin / 123456          管理员
--    operator01 / 123456     普通用户
--    operator02 / 123456     普通用户
DELETE FROM `sys_user`;
INSERT INTO `sys_user` (`id`,`username`,`password`,`real_name`,`role`,`phone`,`email`,`status`) VALUES
(1, 'admin',      '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '系统管理员', 'ADMIN', '13800000000', 'admin@wms.com',   'ENABLED'),
(2, 'operator01', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '张仓管',     'USER',  '13800000001', 'user01@wms.com',  'ENABLED'),
(3, 'operator02', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '李出库',     'USER',  '13800000002', 'user02@wms.com',  'ENABLED');

-- 2. 仓库
DELETE FROM `warehouse`;
INSERT INTO `warehouse` (`id`,`code`,`name`,`address`,`manager`,`phone`,`status`) VALUES
(1, 'WH001', '华东主仓', '上海市青浦区华徐公路 100 号', '王强', '021-60000001', 'ENABLED'),
(2, 'WH002', '华北分仓', '北京市通州区物流园 A 座',     '赵敏', '010-60000002', 'ENABLED');

-- 3. 库位
DELETE FROM `storage_location`;
INSERT INTO `storage_location` (`id`,`warehouse_id`,`code`,`zone`,`status`,`remark`) VALUES
(1, 1, 'A-01-01', 'A', 'EMPTY', '华东主仓 A区 01排 01层'),
(2, 1, 'A-01-02', 'A', 'EMPTY', '华东主仓 A区 01排 02层'),
(3, 1, 'B-01-01', 'B', 'EMPTY', '华东主仓 B区 01排 01层'),
(4, 1, 'B-02-01', 'B', 'EMPTY', '华东主仓 B区 01排 02层'),
(5, 2, 'A-01-01', 'A', 'EMPTY', '华北分仓 A区 01排 01层'),
(6, 2, 'A-02-01', 'A', 'EMPTY', '华北分仓 A区 02排 01层');

-- 4. 商品分类
DELETE FROM `product_category`;
INSERT INTO `product_category` (`id`,`name`,`parent_id`,`sort`) VALUES
(1, '电子元器件', 0, 1),
(2, '包装耗材',   0, 2),
(3, '办公用品',   0, 3);

-- 5. 商品
DELETE FROM `product`;
INSERT INTO `product` (`id`,`sku`,`name`,`category_id`,`spec`,`unit`,`barcode`,`cost_price`,`sale_price`,`status`) VALUES
(1, 'SKU-EC-001', '贴片电阻 10K',   1, '0603 1%',      '个', '690000000001', 0.05,  0.12, 'ENABLED'),
(2, 'SKU-EC-002', '电解电容 470uF', 1, '16V 8*12mm',   '个', '690000000002', 0.35,  0.80, 'ENABLED'),
(3, 'SKU-PK-001', '纸箱 5层瓦楞',   2, '400*300*200',  '个', '690000000003', 3.20,  5.50, 'ENABLED'),
(4, 'SKU-PK-002', '气泡膜',         2, '宽500mm/卷',   '卷', '690000000004', 18.00, 28.00,'ENABLED'),
(5, 'SKU-OF-001', 'A4 打印纸',      3, '70g 500张/包', '包', '690000000005', 19.00, 25.00,'ENABLED');

-- 6. 供应商 / 客户
DELETE FROM `supplier`;
INSERT INTO `supplier` (`id`,`code`,`name`,`contact`,`phone`,`address`,`status`) VALUES
(1, 'SUP001', '华东电子元件有限公司', '陈经理', '021-50000001', '上海市松江区', 'ENABLED'),
(2, 'SUP002', '恒安包装材料厂',       '刘主管', '021-50000002', '上海市奉贤区', 'ENABLED');

DELETE FROM `customer`;
INSERT INTO `customer` (`id`,`code`,`name`,`contact`,`phone`,`address`,`status`) VALUES
(1, 'CUS001', '智联智造科技有限公司', '周厂长', '010-40000001', '北京市亦庄', 'ENABLED'),
(2, 'CUS002', '南方电子贸易商行',     '吴总',   '020-40000002', '广州市天河', 'ENABLED');

-- 7. 库存（初始各库位有货，便于直接演示预警）
DELETE FROM `inventory`;
INSERT INTO `inventory` (`id`,`product_id`,`warehouse_id`,`location_id`,`quantity`,`locked_quantity`,`last_inbound_time`,`last_outbound_time`) VALUES
(1, 1, 1, 1, 5000,  0, NOW(), NOW()),
(2, 2, 1, 2,  800,  0, NOW(), NOW()),
(3, 3, 1, 3,  120,  0, NOW(), NOW()),
(4, 4, 1, 4,   35,  0, NOW(), DATE_SUB(NOW(), INTERVAL 120 DAY)), -- 120天未出库，用于演示呆滞预警
(5, 5, 2, 5,   60,  0, NOW(), NOW());

-- 8. 预警规则（安全库存 / 上限 / 呆滞天数）
DELETE FROM `inventory_alert_rule`;
INSERT INTO `inventory_alert_rule` (`id`,`product_id`,`warehouse_id`,`safety_stock`,`max_stock`,`dead_stock_days`,`enabled`) VALUES
(1, 1, 1, 1000, 20000, 90, 1),  -- 贴片电阻：低于1000告警
(2, 2, 1,  200,  5000, 90, 1),  -- 电解电容：低于200告警
(3, 3, 1,  100,  2000, 60, 1),  -- 纸箱：低于100告警
(4, 4, 1,   50,   500, 90, 1),  -- 气泡膜：低于50告警，且已呆滞
(5, 5, 2,   30,  1000, 90, 1);  -- A4纸：低于30告警

-- 9. 演示用入库单 / 出库单（各一张待审核，登录后即可操作）
DELETE FROM `inbound_order`;
INSERT INTO `inbound_order` (`id`,`order_no`,`type`,`warehouse_id`,`supplier_id`,`status`,`total_quantity`,`total_amount`,`applicant_id`,`remark`) VALUES
(1, 'IN20261012001', 'PURCHASE', 1, 1, 'PENDING', 2000, 100.00, 2, '贴片电阻补货，待管理员审核');

DELETE FROM `inbound_order_item`;
INSERT INTO `inbound_order_item` (`id`,`order_id`,`product_id`,`location_id`,`quantity`,`price`,`amount`) VALUES
(1, 1, 1, 1, 2000, 0.05, 100.00);

DELETE FROM `outbound_order`;
INSERT INTO `outbound_order` (`id`,`order_no`,`type`,`warehouse_id`,`customer_id`,`status`,`total_quantity`,`total_amount`,`applicant_id`,`remark`) VALUES
(1, 'OUT20261012001', 'SALE', 1, 1, 'PENDING', 100, 12.00, 2, '销售出库，待管理员审核');

DELETE FROM `outbound_order_item`;
INSERT INTO `outbound_order_item` (`id`,`order_id`,`product_id`,`location_id`,`quantity`,`price`,`amount`) VALUES
(1, 1, 1, 1, 100, 0.12, 12.00);
