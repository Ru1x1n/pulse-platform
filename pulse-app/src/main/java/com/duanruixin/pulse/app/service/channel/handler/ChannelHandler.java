package com.duanruixin.pulse.app.service.channel.handler;

import com.duanruixin.pulse.app.enums.ChannelType;

/**
 * 渠道发送策略接口
 * 每个渠道类型一个实现,新增渠道 = 新增一个 @Component 实现类,不改任何老代码(开闭原则)
 */
public interface ChannelHandler {

    /** 我负责哪个渠道类型 */
    ChannelType support();

    /**
     * 发送
     * @return true=成功 / false=失败
     */
    boolean send(ChannelSendContext ctx);
}