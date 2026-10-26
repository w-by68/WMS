package com.example.wms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 库存流水表：每一次库存变动都留痕，是对账与审计的依据。
 * before_quantity / after_quantity 记录变动前后账面数量，change_quantity 正增负减。
 */
@Data
@TableName("stock_transaction")
public class StockTransaction {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 流水号：ST + yyyyMMdd + 3 位当日序号 */
    private String transactionNo;

    /** 商品 ID */
    private Long productId;

    /** 仓库 ID */
    private Long warehouseId;

    /** 库位 ID */
    private Long locationId;

    /** 流水类型：INBOUND/OUTBOUND/LOCK/UNLOCK/ADJUST（见 TransactionType） */
    private String bizType;

    /** 关联单号（入库单号/出库单号/盘点单号），便于从流水反查业务 */
    private String bizNo;

    /** 变动量：正数为增加，负数为减少 */
    private Integer changeQuantity;

    /** 变动前账面数量 */
    private Integer beforeQuantity;

    /** 变动后账面数量 */
    private Integer afterQuantity;

    /** 操作人 ID */
    private Long operatorId;

    /** 备注（盘点原因此类） */
    private String remark;

    private LocalDateTime createTime;
}
