package com.duanruixin.pulse.app.mq;

import lombok.Data;

import java.io.Serializable;

/**
 * MQ 发送任务消息体(一个接收者对应一条)
 */
@Data
public class MessageTask implements Serializable {

    /** 全局唯一,雪花 ID(带 MSG_ 前缀),后续幂等/轨迹/死信都靠它 */
    private String messageId;

    /** 来自鉴权拦截器塞进 request 的 appId */
    private Long appId;

    /** 模板编码,如 T001(留着给后续轨迹/统计用) */
    private String templateCode;

    /** 单个接收方(手机号/邮箱),Day13 真发用 */
    private String receiver;

    /** 已渲染好的最终内容,消费端直接发,不用再渲染 */
    private String content;

    /** 入队时间戳 */
    private Long enqueueTime;
}