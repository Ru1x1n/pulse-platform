package com.duanruixin.pulse.app.service.channel.handler;

import com.duanruixin.pulse.app.enums.ChannelType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * App推送渠道(需客户端配合,Demo 保持 Mock)
 */
@Slf4j
@Component
public class PushChannelHandler extends AbstractMockChannelHandler {

    @Override
    public ChannelType support() {
        return ChannelType.PUSH;
    }

    @Override
    protected void doMockSend(ChannelSendContext ctx, boolean success) {
        log.debug("【推送Mock】to={}", ctx.getTask().getReceiver());
        // Day13/后续: 这里调 APNs / FCM / 个推 SDK
    }
}