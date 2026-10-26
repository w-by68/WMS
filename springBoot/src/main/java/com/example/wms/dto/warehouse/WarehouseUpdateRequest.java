package com.example.wms.dto.warehouse;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 修改仓库请求 */
@Data
public class WarehouseUpdateRequest {

    @NotBlank(message = "仓库名称不能为空")
    private String name;

    private String address;

    private String manager;

    private String phone;
}
