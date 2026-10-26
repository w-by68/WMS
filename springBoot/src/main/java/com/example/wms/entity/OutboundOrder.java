package com.example.wms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 出库单主表（库存锁定模型）：
 * 提交时锁定库存（locked_quantity += n，写 LOCK 流水），
 * 审核通过扣减实际库存并释放锁定（写 OUTBOUND 流水），
 * 驳回/作废仅释放锁定（写 UNLOCK 流水）。
 */
@Data
@TableName("outbound_order")
public class OutboundOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 出库单号：OUT + yyyyMMdd + 3 位当日序号，全局唯一 */
    private String orderNo;

    /** 出库类型：SALE-销售 PICK-领用 SCRAP-报损 */
    private String type;

    /** 出库仓库 ID */
    private Long warehouseId;

    /** 客户 ID（销售出库时必填） */
    private Long customerId;

    /** 状态：DRAFT/PENDING/APPROVED/REJECTED/CANCELED（见 OrderStatus） */
    private String status;

    /** 明细合计数量 */
    private Integer totalQuantity;

    /** 明细合计金额 */
    private BigDecimal totalAmount;

    /** 制单人 */
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
