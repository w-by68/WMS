package com.example.wms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 客户表：销售出库单的往来单位。
 */
@Data
@TableName("customer")
public class Customer {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 客户编码，全局唯一 */
    private String code;

    /** 客户名称 */
    private String name;

    /** 联系人 */
    private String contact;

    private String phone;

    private String address;

    /** 状态：ENABLED-启用 DISABLED-停用 */
    private String status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
