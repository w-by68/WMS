package com.example.wms.dto.location;

import lombok.Data;

/** 修改库位请求 */
@Data
public class StorageLocationUpdateRequest {

    private String zone;

    private String remark;
}
