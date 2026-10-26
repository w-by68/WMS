package com.example.wms.dto.customer;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 创建客户请求 */
@Data
public class CustomerCreateRequest {

    @NotBlank(message = "客户编码不能为空")
    private String code;

    @NotBlank(message = "客户名称不能为空")
    private String name;

    private String contact;

    private String phone;

    private String address;

    private String status;
}
