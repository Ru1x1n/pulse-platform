package com.duanruixin.pulse.app.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_message")
public class Message {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String messageId;
    private Long appId;
    private String templateCode;
    private String receiver;
    private String content;
    private Integer channelType;
    private Integer status;
    private Integer retryCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}