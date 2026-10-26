package com.example.wms.dto.location;

import lombok.Data;

/** 库位分页查询条件 */
@Data
public class StorageLocationQueryDTO {

    private Integer pageNum = 1;

    private Integer pageSize = 10;

    /** 仓库 ID 过滤 */
    private Long warehouseId;

    /** 库区过滤 */
    private String zone;

    /** 状态过滤：EMPTY / USING / DISABLED */
    private String status;

    /** 模糊匹配库位编码 */
    private String keyword;
}
