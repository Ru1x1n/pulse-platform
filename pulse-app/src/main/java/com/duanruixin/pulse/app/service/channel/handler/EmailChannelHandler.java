package com.duanruixin.pulse.app.service.channel.handler;

import com.duanruixin.pulse.app.enums.ChannelType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 邮件渠道(Day13 换 JavaMail SMTP 真发——这是 Demo 主推的真渠道)
 */
@Slf4j
@Component
public class EmailChannelHandler extends AbstractMockChannelHandler {

    @Override
    public ChannelType support() {
        return ChannelType.EMAIL;
    }

    @Override
    protected void doMockSend(ChannelSendContext ctx, boolean success) {
        log.debug("【邮件Mock】to={}, content={}",
                ctx.getTask().getReceiver(), ctx.getTask().getContent());
        // Day13: 这里换 JavaMailSender 真发邮件
    }
}