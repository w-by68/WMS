package com.example.wms.vo.supplier;

import lombok.Data;

import java.time.LocalDateTime;

/** 供应商 VO */
@Data
public class SupplierVO {

    private Long id;

    private String code;

    private String name;

    private String contact;

    private String phone;

    private String address;

    private String status;

    private LocalDateTime createTime;
}
