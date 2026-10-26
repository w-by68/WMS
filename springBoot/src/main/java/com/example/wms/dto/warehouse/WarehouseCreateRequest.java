package com.example.wms.dto.warehouse;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 创建仓库请求 */
@Data
public class WarehouseCreateRequest {

    @NotBlank(message = "仓库编码不能为空")
    private String code;

    @NotBlank(message = "仓库名称不能为空")
    private String name;

    private String address;

    private String manager;

    private String phone;

    /** ENABLED / DISABLED，默认 ENABLED */
    private String status;
}
