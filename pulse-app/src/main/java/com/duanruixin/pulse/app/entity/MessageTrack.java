package com.duanruixin.pulse.app.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_message_track")
public class MessageTrack {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String messageId;
    private Integer fromStatus;
    private Integer toStatus;
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}