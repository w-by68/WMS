-- =============================================================
-- 智能仓储管理系统（WMS） 数据库结构
-- MySQL 8.0 / InnoDB / utf8mb4
-- 执行顺序：本文件 -> 02-init-data.sql
-- =============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 1. 用户表（系统不提供注册入口，账号全部由管理员创建）
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `id`              bigint       NOT NULL AUTO_INCREMENT,
  `username`        varchar(50)  NOT NULL                COMMENT '登录账号，全局唯一',
  `password`        varchar(100) NOT NULL                COMMENT 'BCrypt 加密后的密码',
  `real_name`       varchar(50)  DEFAULT NULL            COMMENT '真实姓名',
  `role`            varchar(20)  NOT NULL DEFAULT 'USER' COMMENT '角色：ADMIN-管理员 USER-普通用户',
  `phone`           varchar(20)  DEFAULT NULL,
  `email`           varchar(100) DEFAULT NULL,
  `status`          varchar(20)  NOT NULL DEFAULT 'ENABLED' COMMENT '状态：ENABLED-启用 DISABLED-禁用',
  `last_login_time` datetime     DEFAULT NULL            COMMENT '最近登录时间',
  `create_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`         tinyint(1)   NOT NULL DEFAULT 0      COMMENT '逻辑删除：0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_role_status` (`role`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 2. 仓库表
DROP TABLE IF EXISTS `warehouse`;
CREATE TABLE `warehouse` (
  `id`          bigint      NOT NULL AUTO_INCREMENT,
  `code`        varchar(32) NOT NULL                COMMENT '仓库编码，如 WH001',
  `name`        varchar(100) NOT NULL               COMMENT '仓库名称',
  `address`     varchar(255) DEFAULT NULL,
  `manager`     varchar(50)  DEFAULT NULL           COMMENT '仓库负责人',
  `phone`       varchar(20)  DEFAULT NULL,
  `status`      varchar(20)  NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED-启用 DISABLED-停用',
  `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`     tinyint(1)   NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='仓库表';

-- 3. 库位表（归属于某个仓库，编码格式建议：A-01-02 表示 A区-01排-02层）
DROP TABLE IF EXISTS `storage_location`;
CREATE TABLE `storage_location` (
  `id`           bigint      NOT NULL AUTO_INCREMENT,
  `warehouse_id` bigint      NOT NULL                COMMENT '所属仓库ID',
  `code`         varchar(32) NOT NULL                COMMENT '库位编码，仓库内唯一',
  `zone`         varchar(20)  DEFAULT NULL           COMMENT '库区：A/B/C',
  `status`       varchar(20)  NOT NULL DEFAULT 'EMPTY' COMMENT 'EMPTY-空闲 USING-占用 DISABLED-禁用',
  `remark`       varchar(255) DEFAULT NULL,
  `create_time`  datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`  datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`      tinyint(1)  NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_wh_code` (`warehouse_id`, `code`),
  KEY `idx_warehouse` (`warehouse_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库位表';

-- 4. 商品分类表
DROP TABLE IF EXISTS `product_category`;
CREATE TABLE `product_category` (
  `id`          bigint      NOT NULL AUTO_INCREMENT,
  `name`        varchar(100) NOT NULL,
  `parent_id`   bigint      NOT NULL DEFAULT 0 COMMENT '父级ID，0表示一级分类',
  `sort`        int         NOT NULL DEFAULT 0,
  `create_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`     tinyint(1)  NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_parent` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品分类表';

-- 5. 商品表（SKU 维度）
DROP TABLE IF EXISTS `product`;
CREATE TABLE `product` (
  `id`           bigint       NOT NULL AUTO_INCREMENT,
  `sku`          varchar(64)  NOT NULL                COMMENT 'SKU 编码，全局唯一',
  `name`         varchar(200) NOT NULL                COMMENT '商品名称',
  `category_id`  bigint       DEFAULT NULL            COMMENT '分类ID',
  `spec`         varchar(200) DEFAULT NULL            COMMENT '规格型号',
  `unit`         varchar(20)  DEFAULT NULL            COMMENT '计量单位：个/箱/kg',
  `barcode`      varchar(64)  DEFAULT NULL            COMMENT '条码',
  `cost_price`   decimal(12,2) NOT NULL DEFAULT 0.00  COMMENT '成本价（参考）',
  `sale_price`   decimal(12,2) NOT NULL DEFAULT 0.00  COMMENT '售价（参考）',
  `status`       varchar(20)  NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED-启用 DISABLED-停用',
  `create_time`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`      tinyint(1)   NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sku` (`sku`),
  KEY `idx_category` (`category_id`),
  KEY `idx_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

-- 6. 供应商表
DROP TABLE IF EXISTS `supplier`;
CREATE TABLE `supplier` (
  `id`          bigint      NOT NULL AUTO_INCREMENT,
  `code`        varchar(32) NOT NULL COMMENT '供应商编码',
  `name`        varchar(100) NOT NULL,
  `contact`     varchar(50)  DEFAULT NULL COMMENT '联系人',
  `phone`       varchar(20)  DEFAULT NULL,
  `address`     varchar(255) DEFAULT NULL,
  `status`      varchar(20)  NOT NULL DEFAULT 'ENABLED',
  `create_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`     tinyint(1)  NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='供应商表';

-- 7. 客户表
DROP TABLE IF EXISTS `customer`;
CREATE TABLE `customer` (
  `id`          bigint      NOT NULL AUTO_INCREMENT,
  `code`        varchar(32) NOT NULL COMMENT '客户编码',
  `name`        varchar(100) NOT NULL,
  `contact`     varchar(50)  DEFAULT NULL,
  `phone`       varchar(20)  DEFAULT NULL,
  `address`     varchar(255) DEFAULT NULL,
  `status`      varchar(20)  NOT NULL DEFAULT 'ENABLED',
  `create_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`     tinyint(1)  NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户表';

-- 8. 库存表（核心表：商品 x 库位 唯一）
DROP TABLE IF EXISTS `inventory`;
CREATE TABLE `inventory` (
  `id`              bigint   NOT NULL AUTO_INCREMENT,
  `product_id`      bigint   NOT NULL                COMMENT '商品ID',
  `warehouse_id`    bigint   NOT NULL                COMMENT '仓库ID',
  `location_id`     bigint   NOT NULL                COMMENT '库位ID',
  `quantity`        int      NOT NULL DEFAULT 0      COMMENT '账面库存数量',
  `locked_quantity` int      NOT NULL DEFAULT 0      COMMENT '锁定数量（出库单待审核占用）',
  `version`         int      NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
  `last_inbound_time`  datetime DEFAULT NULL         COMMENT '最近入库时间',
  `last_outbound_time` datetime DEFAULT NULL         COMMENT '最近出库时间（用于呆滞判断）',
  `create_time`     datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`     datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_product_location` (`product_id`, `location_id`),
  KEY `idx_product` (`product_id`),
  KEY `idx_warehouse` (`warehouse_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存表（可用库存 = quantity - locked_quantity）';

-- 9. 库存预警规则表（"智能"的核心配置）
DROP TABLE IF EXISTS `inventory_alert_rule`;
CREATE TABLE `inventory_alert_rule` (
  `id`             bigint  NOT NULL AUTO_INCREMENT,
  `product_id`     bigint  NOT NULL           COMMENT '商品ID',
  `warehouse_id`   bigint  DEFAULT NULL       COMMENT '仓库ID，NULL 表示全仓库通用',
  `safety_stock`   int     NOT NULL DEFAULT 0 COMMENT '安全库存下限，低于此值触发缺货预警',
  `max_stock`      int     NOT NULL DEFAULT 0 COMMENT '库存上限，高于此值触发积压预警；0 表示不限制',
  `dead_stock_days` int    NOT NULL DEFAULT 90 COMMENT '呆滞天数：超过该天数无出库则预警',
  `enabled`        tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  `create_time`    datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`    datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_product_warehouse` (`product_id`, `warehouse_id`),
  KEY `idx_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存预警规则表';

-- 10. 库存预警记录表（定时任务 / 库存变动触发后落库）
DROP TABLE IF EXISTS `inventory_alert`;
CREATE TABLE `inventory_alert` (
  `id`              bigint   NOT NULL AUTO_INCREMENT,
  `alert_no`        varchar(32) NOT NULL           COMMENT '预警单号 AL+年月日+序号',
  `rule_id`         bigint   DEFAULT NULL,
  `product_id`      bigint   NOT NULL,
  `warehouse_id`    bigint   NOT NULL,
  `alert_type`      varchar(20) NOT NULL           COMMENT 'SHORTAGE-缺货 OVERSTOCK-积压 DEAD_STOCK-呆滞',
  `level`           varchar(20) NOT NULL DEFAULT 'WARNING' COMMENT 'INFO-提示 WARNING-警告 CRITICAL-严重',
  `current_quantity` int     NOT NULL DEFAULT 0    COMMENT '预警触发时的可用库存',
  `threshold_value`  int     NOT NULL DEFAULT 0    COMMENT '触发阈值',
  `suggest_quantity` int     NOT NULL DEFAULT 0    COMMENT '智能建议：缺货=建议补货量，积压=建议消化量',
  `status`          varchar(20) NOT NULL DEFAULT 'UNRESOLVED' COMMENT 'UNRESOLVED-未处理 RESOLVED-已处理 IGNORED-已忽略',
  `handler_id`      bigint   DEFAULT NULL          COMMENT '处理人',
  `handle_time`     datetime DEFAULT NULL,
  `handle_remark`   varchar(500) DEFAULT NULL,
  `create_time`     datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`     datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_alert_no` (`alert_no`),
  KEY `idx_status_type` (`status`, `alert_type`),
  KEY `idx_product` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存预警记录表';

-- 11. 入库单主表
DROP TABLE IF EXISTS `inbound_order`;
CREATE TABLE `inbound_order` (
  `id`            bigint      NOT NULL AUTO_INCREMENT,
  `order_no`      varchar(32) NOT NULL           COMMENT '入库单号 IN+年月日+序号',
  `type`          varchar(20) NOT NULL DEFAULT 'PURCHASE' COMMENT 'PURCHASE-采购入库 RETURN-退货入库 ADJUST-调整入库',
  `warehouse_id`  bigint      NOT NULL,
  `supplier_id`   bigint      DEFAULT NULL,
  `status`        varchar(20) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT-草稿 PENDING-待审核 APPROVED-已审核生效 REJECTED-已驳回 CANCELED-已作废',
  `total_quantity` int        NOT NULL DEFAULT 0,
  `total_amount`  decimal(14,2) NOT NULL DEFAULT 0.00,
  `applicant_id`  bigint      NOT NULL           COMMENT '制单人',
  `auditor_id`    bigint      DEFAULT NULL       COMMENT '审核人（管理员）',
  `audit_time`    datetime    DEFAULT NULL,
  `audit_remark`  varchar(500) DEFAULT NULL,
  `remark`        varchar(500) DEFAULT NULL,
  `create_time`   datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`   datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`       tinyint(1)  NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_status` (`status`),
  KEY `idx_applicant` (`applicant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='入库单表';

-- 12. 入库单明细表
DROP TABLE IF EXISTS `inbound_order_item`;
CREATE TABLE `inbound_order_item` (
  `id`           bigint   NOT NULL AUTO_INCREMENT,
  `order_id`     bigint   NOT NULL,
  `product_id`   bigint   NOT NULL,
  `location_id`  bigint   NOT NULL           COMMENT '入库库位',
  `quantity`     int      NOT NULL,
  `price`        decimal(12,2) NOT NULL DEFAULT 0.00,
  `amount`       decimal(14,2) NOT NULL DEFAULT 0.00,
  `create_time`  datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_order` (`order_id`),
  KEY `idx_product` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='入库单明细表';

-- 13. 出库单主表
DROP TABLE IF EXISTS `outbound_order`;
CREATE TABLE `outbound_order` (
  `id`            bigint      NOT NULL AUTO_INCREMENT,
  `order_no`      varchar(32) NOT NULL           COMMENT '出库单号 OUT+年月日+序号',
  `type`          varchar(20) NOT NULL DEFAULT 'SALE' COMMENT 'SALE-销售出库 PICK-领用出库 SCRAP-报损出库',
  `warehouse_id`  bigint      NOT NULL,
  `customer_id`   bigint      DEFAULT NULL,
  `status`        varchar(20) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT-草稿 PENDING-待审核（已锁定库存） APPROVED-已审核出库 REJECTED-已驳回 CANCELED-已作废',
  `total_quantity` int        NOT NULL DEFAULT 0,
  `total_amount`  decimal(14,2) NOT NULL DEFAULT 0.00,
  `applicant_id`  bigint      NOT NULL,
  `auditor_id`    bigint      DEFAULT NULL,
  `audit_time`    datetime    DEFAULT NULL,
  `audit_remark`  varchar(500) DEFAULT NULL,
  `remark`        varchar(500) DEFAULT NULL,
  `create_time`   datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`   datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`       tinyint(1)  NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_status` (`status`),
  KEY `idx_applicant` (`applicant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='出库单表';

-- 14. 出库单明细表
DROP TABLE IF EXISTS `outbound_order_item`;
CREATE TABLE `outbound_order_item` (
  `id`          bigint   NOT NULL AUTO_INCREMENT,
  `order_id`    bigint   NOT NULL,
  `product_id`  bigint   NOT NULL,
  `location_id` bigint   NOT NULL           COMMENT '出货库位',
  `quantity`    int      NOT NULL,
  `price`       decimal(12,2) NOT NULL DEFAULT 0.00,
  `amount`      decimal(14,2) NOT NULL DEFAULT 0.00,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_order` (`order_id`),
  KEY `idx_product` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='出库单明细表';

-- 15. 库存流水表（每一次库存变动都留痕，对账用）
DROP TABLE IF EXISTS `stock_transaction`;
CREATE TABLE `stock_transaction` (
  `id`              bigint   NOT NULL AUTO_INCREMENT,
  `transaction_no`  varchar(32) NOT NULL      COMMENT '流水号 ST+时间戳',
  `product_id`      bigint   NOT NULL,
  `warehouse_id`    bigint   NOT NULL,
  `location_id`     bigint   NOT NULL,
  `biz_type`        varchar(20) NOT NULL      COMMENT 'INBOUND-入库 OUTBOUND-出库 LOCK-锁定 UNLOCK-解锁 ADJUST-盘点调整',
  `biz_no`          varchar(32) DEFAULT NULL  COMMENT '关联单号',
  `change_quantity` int      NOT NULL         COMMENT '变动量，正数为增加，负数为减少',
  `before_quantity` int      NOT NULL,
  `after_quantity`  int      NOT NULL,
  `operator_id`     bigint   DEFAULT NULL,
  `remark`          varchar(500) DEFAULT NULL,
  `create_time`     datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_product_time` (`product_id`, `create_time`),
  KEY `idx_biz_no` (`biz_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存流水表';

-- 16. 操作日志表（AOP 自动记录，管理员可查）
DROP TABLE IF EXISTS `sys_operation_log`;
CREATE TABLE `sys_operation_log` (
  `id`           bigint   NOT NULL AUTO_INCREMENT,
  `user_id`      bigint   DEFAULT NULL,
  `username`     varchar(50) DEFAULT NULL,
  `module`       varchar(50) DEFAULT NULL COMMENT '模块名',
  `operation`    varchar(100) DEFAULT NULL COMMENT '操作描述',
  `method`       varchar(200) DEFAULT NULL COMMENT '请求方法',
  `request_url`  varchar(255) DEFAULT NULL,
  `ip`           varchar(50) DEFAULT NULL,
  `cost_ms`      int      DEFAULT NULL     COMMENT '耗时毫秒',
  `success`      tinyint(1) NOT NULL DEFAULT 1,
  `error_msg`    varchar(1000) DEFAULT NULL,
  `create_time`  datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_time` (`user_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

SET FOREIGN_KEY_CHECKS = 1;
