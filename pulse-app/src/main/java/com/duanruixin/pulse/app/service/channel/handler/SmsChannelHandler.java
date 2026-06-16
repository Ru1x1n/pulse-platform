package com.duanruixin.pulse.app.service.channel.handler;

import com.duanruixin.pulse.app.enums.ChannelType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 短信渠道(Day13 换阿里云/腾讯云 SDK;短信因签名需企业资质,生产真发,Demo 保持 Mock)
 */
@Slf4j
@Component
public class SmsChannelHandler extends AbstractMockChannelHandler {

    @Override
    public ChannelType support() {
        return ChannelType.SMS;
    }

    @Override
    protected void doMockSend(ChannelSendContext ctx, boolean success) {
        log.debug("【短信Mock】to={}, content={}",
                ctx.getTask().getReceiver(), ctx.getTask().getContent());
        // Day13: 这里调 aliyun/tencent 短信 SDK
    }
}