package com.duanruixin.pulse.app.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("t_channel_config")
public class ChannelConfig implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long appId;
    private Integer channelType;
    private String channelName;
    private String provider;
    private String configJson;
    private Integer priority;
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}