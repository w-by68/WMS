package com.example.wms.dto.supplier;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 修改供应商请求 */
@Data
public class SupplierUpdateRequest {

    @NotBlank(message = "供应商名称不能为空")
    private String name;

    private String contact;

    private String phone;

    private String address;
}
