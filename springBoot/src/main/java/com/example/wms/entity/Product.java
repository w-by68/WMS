package com.example.wms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品表（SKU 维度）：出入库单明细的挂靠主体。
 */
@Data
@TableName("product")
public class Product {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** SKU 编码，全局唯一（业务上的商品身份证） */
    private String sku;

    /** 商品名称 */
    private String name;

    /** 分类 ID，关联 product_category */
    private Long categoryId;

    /** 规格型号，如 0603 1% */
    private String spec;

    /** 计量单位：个/箱/kg/卷 */
    private String unit;

    /** 商品条码 */
    private String barcode;

    /** 成本价（参考），金额一律 BigDecimal */
    private BigDecimal costPrice;

    /** 售价（参考） */
    private BigDecimal salePrice;

    /** 状态：ENABLED-启用 DISABLED-停用（停用商品不再出现在下拉中） */
    private String status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
