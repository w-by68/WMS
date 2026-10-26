package com.example.wms.vo.warehouse;

import lombok.Data;

import java.time.LocalDateTime;

/** 仓库 VO */
@Data
public class WarehouseVO {

    private Long id;

    private String code;

    private String name;

    private String address;

    private String manager;

    private String phone;

    private String status;

    private LocalDateTime createTime;
}
