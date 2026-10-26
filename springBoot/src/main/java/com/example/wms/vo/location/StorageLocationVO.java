package com.example.wms.vo.location;

import lombok.Data;

import java.time.LocalDateTime;

/** 库位 VO（带仓库名称） */
@Data
public class StorageLocationVO {

    private Long id;

    private Long warehouseId;

    private String warehouseName;

    private String code;

    private String zone;

    private String status;

    private String remark;

    private LocalDateTime createTime;
}
