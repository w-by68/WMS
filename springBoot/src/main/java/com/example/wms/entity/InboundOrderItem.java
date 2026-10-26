package com.example.wms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 入库单明细表：随主表创建/修改整体替换（先删后插），审核通过时逐行增加库存。
 */
@Data
@TableName("inbound_order_item")
public class InboundOrderItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属入库单 ID */
    private Long orderId;

    /** 商品 ID */
    private Long productId;

    /** 入库目标库位 ID（必须属于主表的仓库） */
    private Long locationId;

    /** 入库数量，必须 > 0 */
    private Integer quantity;

    /** 入库单价，金额一律 BigDecimal */
    private BigDecimal price;

    /** 行金额 = price * quantity，保留 2 位 */
    private BigDecimal amount;

    private LocalDateTime createTime;
}
