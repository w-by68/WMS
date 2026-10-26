package com.example.wms.dto.user;

import lombok.Data;

/** 用户分页查询条件 */
@Data
public class UserQueryDTO {

    /** 当前页，默认 1 */
    private Integer pageNum = 1;

    /** 每页大小，默认 10 */
    private Integer pageSize = 10;

    /** 模糊匹配用户名或姓名 */
    private String keyword;

    /** 角色过滤 */
    private String role;

    /** 状态过滤 */
    private String status;
}
