package com.duanruixin.pulse.app.service.channel.handler;

import com.duanruixin.pulse.app.enums.ChannelType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 微信公众号渠道(需认证服务号,Demo 保持 Mock)
 */
@Slf4j
@Component
public class WechatChannelHandler extends AbstractMockChannelHandler {

    @Override
    public ChannelType support() {
        return ChannelType.WECHAT;
    }

    @Override
    protected void doMockSend(ChannelSendContext ctx, boolean success) {
        log.debug("【微信Mock】to={}", ctx.getTask().getReceiver());
        // 后续: 这里调微信公众号模板消息 API
    }
}