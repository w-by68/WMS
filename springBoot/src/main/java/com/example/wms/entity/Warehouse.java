package com.example.wms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 仓库表：库位的上级归属，删除前需校验其下没有库位（03 阶段实现引用检查）。
 */
@Data
@TableName("warehouse")
public class Warehouse {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 仓库编码，如 WH001，全局唯一 */
    private String code;

    /** 仓库名称 */
    private String name;

    /** 仓库地址 */
    private String address;

    /** 仓库负责人 */
    private String manager;

    private String phone;

    /** 状态：ENABLED-启用 DISABLED-停用 */
    private String status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
