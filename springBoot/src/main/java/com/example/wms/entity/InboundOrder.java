package com.example.wms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 入库单主表：审核通过时增加库存并写 INBOUND 流水（入库不锁库存）。
 */
@Data
@TableName("inbound_order")
public class InboundOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 入库单号：IN + yyyyMMdd + 3 位当日序号，全局唯一 */
    private String orderNo;

    /** 入库类型：PURCHASE-采购 RETURN-退货 ADJUST-调整 */
    private String type;

    /** 入库仓库 ID */
    private Long warehouseId;

    /** 供应商 ID（采购入库时必填） */
    private Long supplierId;

    /** 状态：DRAFT/PENDING/APPROVED/REJECTED/CANCELED（见 OrderStatus） */
    private String status;

    /** 明细合计数量（创建/修改时自动计算） */
    private Integer totalQuantity;

    /** 明细合计金额，amount = price * quantity */
    private BigDecimal totalAmount;

    /** 制单人（普通用户），USER 查列表时按此字段过滤数据范围 */
    private Long applicantId;

    /** 审核人（管理员） */
    private Long auditorId;

    /** 审核时间 */
    private LocalDateTime auditTime;

    /** 审核备注（驳回时必填原因） */
    private String auditRemark;

    /** 制单备注 */
    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
