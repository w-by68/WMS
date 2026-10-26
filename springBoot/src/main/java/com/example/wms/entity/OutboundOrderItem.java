package com.example.wms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 出库单明细表：提交时逐行校验可用库存并锁定，审核通过逐行扣减。
 */
@Data
@TableName("outbound_order_item")
public class OutboundOrderItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属出库单 ID */
    private Long orderId;

    /** 商品 ID */
    private Long productId;

    /** 出货库位 ID（必须属于主表的仓库） */
    private Long locationId;

    /** 出库数量，必须 > 0 且 <= 提交时刻的可用库存 */
    private Integer quantity;

    /** 出库单价 */
    private BigDecimal price;

    /** 行金额 = price * quantity */
    private BigDecimal amount;

    private LocalDateTime createTime;
}
