package com.duanruixin.pulse.app.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("t_tenant")
public class Tenant implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String tenantName;

    private String tenantCode;

    private String contactName;

    private String contactEmail;

    private String contactPhone;

    /** 状态:1-正常 0-停用 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}