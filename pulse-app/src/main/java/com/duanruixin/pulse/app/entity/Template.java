package com.duanruixin.pulse.app.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("t_template")
public class Template implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String templateCode;
    private Long appId;
    private String templateName;
    private Integer channelType;
    private String content;
    private String variables;  // JSON 字符串
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}