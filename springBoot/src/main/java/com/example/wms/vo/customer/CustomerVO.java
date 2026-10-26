package com.example.wms.vo.customer;

import lombok.Data;

import java.time.LocalDateTime;

/** 客户 VO */
@Data
public class CustomerVO {

    private Long id;

    private String code;

    private String name;

    private String contact;

    private String phone;

    private String address;

    private String status;

    private LocalDateTime createTime;
}
