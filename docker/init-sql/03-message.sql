-- t_message 消息主表(按月分表预留:无外键 + message_id 含时间戳 + create_time 索引)
CREATE TABLE IF NOT EXISTS t_message (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    message_id    VARCHAR(64)  NOT NULL COMMENT '业务消息ID(MSG_+雪花),全局唯一',
    app_id        BIGINT       NOT NULL COMMENT '应用ID',
    template_code VARCHAR(64)  NOT NULL COMMENT '模板编码',
    receiver      VARCHAR(64)  NOT NULL COMMENT '接收方(手机号/邮箱)',
    content       VARCHAR(1024) NOT NULL COMMENT '渲染后的最终内容',
    channel_type  TINYINT      DEFAULT NULL COMMENT '渠道类型(预留,Day12)',
    status        TINYINT      NOT NULL DEFAULT 0 COMMENT '0待发 1发送中 2成功 3失败 4退回',
    retry_count   INT          NOT NULL DEFAULT 0 COMMENT '重试次数(预留,Day15)',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted    TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_message_id (message_id),
    KEY idx_app_receiver (app_id, receiver),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息主表';

-- t_message_track 轨迹表(状态流转流水,Canal→ES 也会监听它/主表)
CREATE TABLE IF NOT EXISTS t_message_track (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    message_id  VARCHAR(64)  NOT NULL COMMENT '关联消息ID',
    from_status TINYINT      DEFAULT NULL COMMENT '原状态(创建时为空)',
    to_status   TINYINT      NOT NULL COMMENT '新状态',
    remark      VARCHAR(255) DEFAULT NULL COMMENT '备注',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted  TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_message_id (message_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息轨迹表';