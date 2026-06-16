package com.duanruixin.pulse.app.service.channel;

import com.duanruixin.pulse.app.mq.MessageTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Mock 渠道发送器(Day11)
 * Day13 换成真实渠道 SDK(阿里云短信 / JavaMail 等)
 * 当前:按 ChannelFaultInjector 的失败率随机判定成功/失败
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MockChannelSender {

    private final ChannelFaultInjector faultInjector;

    /**
     * 模拟发送,返回 true=成功 / false=失败
     */
    public boolean send(Integer channelType, String provider, MessageTask task) {
        double failRate = faultInjector.getFailRate(channelType, provider);
        boolean success = ThreadLocalRandom.current().nextDouble() >= failRate;
        log.info("【Mock发送】provider={}, messageId={}, 失败率={}, 结果={}",
                provider, task.getMessageId(), failRate, success ? "成功" : "失败");
        return success;
    }
}