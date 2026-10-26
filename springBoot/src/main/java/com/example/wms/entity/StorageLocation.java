package com.example.wms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 库位表：库存的最小物理单元，编码建议"区-排-层"（如 A-01-02），仓库内唯一。
 */
@Data
@TableName("storage_location")
public class StorageLocation {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属仓库 ID */
    private Long warehouseId;

    /** 库位编码，仓库内唯一（联合唯一索引 uk_wh_code：warehouse_id + code） */
    private String code;

    /** 库区：A/B/C */
    private String zone;

    /** 状态：EMPTY-空闲 USING-占用 DISABLED-禁用（随库存自动维护） */
    private String status;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
