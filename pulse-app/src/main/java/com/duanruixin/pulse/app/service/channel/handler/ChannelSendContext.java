package com.duanruixin.pulse.app.service.channel.handler;

import com.duanruixin.pulse.app.entity.ChannelConfig;
import com.duanruixin.pulse.app.mq.MessageTask;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 渠道发送上下文(策略 send 的统一入参)
 * 以后要加发送所需字段,只改这里,不动各 Handler 签名
 */
@Data
@AllArgsConstructor
public class ChannelSendContext {

    /** 路由选中的渠道配置(含 provider / configJson,Day13 真发要用 configJson 里的密钥) */
    private ChannelConfig channelConfig;

    /** MQ 任务(含 messageId / receiver / content) */
    private MessageTask task;

    /** 便捷方法:取渠道类型 */
    public Integer getChannelType() {
        return channelConfig.getChannelType();
    }

    /** 便捷方法:取服务商 */
    public String getProvider() {
        return channelConfig.getProvider();
    }
}