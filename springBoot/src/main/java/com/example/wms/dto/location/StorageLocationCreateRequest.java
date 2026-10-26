package com.example.wms.dto.location;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 创建库位请求 */
@Data
public class StorageLocationCreateRequest {

    @NotNull(message = "所属仓库不能为空")
    private Long warehouseId;

    @NotBlank(message = "库位编码不能为空")
    private String code;

    private String zone;

    private String remark;
}
