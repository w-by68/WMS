package com.example.wms.dto.customer;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 修改客户请求 */
@Data
public class CustomerUpdateRequest {

    @NotBlank(message = "客户名称不能为空")
    private String name;

    private String contact;

    private String phone;

    private String address;
}
