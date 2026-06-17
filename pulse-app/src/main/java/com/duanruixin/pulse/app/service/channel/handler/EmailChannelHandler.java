package com.duanruixin.pulse.app.service.channel.handler;

import com.duanruixin.pulse.app.enums.ChannelType;
import com.duanruixin.pulse.app.service.channel.ChannelFaultInjector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 邮件渠道(Day13 真发:JavaMail + QQ邮箱 SMTP)
 * 不继承 AbstractMockChannelHandler——真发成败由发送结果决定,不能用随机判定
 * 仍保留故障注入开关:手动注入失败率时模拟失败(供降级测试),失败率为0时真发
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailChannelHandler implements ChannelHandler {

    private final JavaMailSender mailSender;
    private final ChannelFaultInjector faultInjector;

    /** 发件人(必须 = yml 里配的 username,否则 QQ 拒发) */
    @Value("${spring.mail.username}")
    private String from;

    @Override
    public ChannelType support() {
        return ChannelType.EMAIL;
    }

    @Override
    public boolean send(ChannelSendContext ctx) {
        Integer channelType = ctx.getChannelType();
        String provider = ctx.getProvider();
        String to = ctx.getTask().getReceiver();
        String messageId = ctx.getTask().getMessageId();

        // 故障注入开关:手动注入失败率时模拟失败(供 Day11 降级测试),不真发
        double failRate = faultInjector.getFailRate(channelType, provider);
        if (failRate > 0 && ThreadLocalRandom.current().nextDouble() < failRate) {
            log.warn("【邮件发送-故障注入】provider={}, messageId={}, 失败率={} → 模拟失败",
                    provider, messageId, failRate);
            return false;
        }

        // 真实发送
        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom(from);
            mail.setTo(to);
            mail.setSubject("【Pulse】消息通知");          // Day13 先固定标题，后续可模板化
            mail.setText(ctx.getTask().getContent());      // 已渲染好的内容
            mailSender.send(mail);

            log.info("【邮件发送】provider={}, messageId={}, to={}, 结果=成功",
                    provider, messageId, to);
            return true;
        } catch (Exception e) {
            log.error("【邮件发送】provider={}, messageId={}, to={}, 结果=失败, err={}",
                    provider, messageId, to, e.getMessage());
            return false;
        }
    }
}