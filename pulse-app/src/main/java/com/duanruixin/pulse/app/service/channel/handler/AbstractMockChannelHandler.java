package com.duanruixin.pulse.app.service.channel.handler;

import com.duanruixin.pulse.app.service.channel.ChannelFaultInjector;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Mock 渠道 Handler 抽象基类
 * 统一处理「故障注入 + 随机判定成败」,子类只实现 doMockSend(真实发送时这里换成 SDK 调用)
 * Day13 真渠道:对应渠道的 Handler 重写 doMockSend 为真实发送即可
 */
@Slf4j
public abstract class AbstractMockChannelHandler implements ChannelHandler {

    /**
     * 故障注入器用字段注入(基类用构造注入会牵连所有子类写构造器,这里破例用 @Autowired)
     * 注:项目默认构造注入,此处为抽象基类共享依赖的特例
     */
    @Autowired
    protected ChannelFaultInjector faultInjector;

    @Override
    public boolean send(ChannelSendContext ctx) {
        Integer channelType = ctx.getChannelType();
        String provider = ctx.getProvider();

        // 故障注入:按设定失败率随机判定(基类统一,子类不用管)
        double failRate = faultInjector.getFailRate(channelType, provider);
        boolean success = ThreadLocalRandom.current().nextDouble() >= failRate;

        // 子类填:这个渠道发送时的具体动作(Mock 阶段只打日志;Day13 换真 SDK)
        doMockSend(ctx, success);

        log.info("【{}发送】provider={}, messageId={}, 失败率={}, 结果={}",
                support().getDesc(), provider, ctx.getTask().getMessageId(),
                failRate, success ? "成功" : "失败");
        return success;
    }

    /**
     * 子类实现:具体渠道的发送动作
     * Mock 阶段一般只打个渠道特有的日志;Day13 真渠道在此调 SDK / SMTP
     */
    protected abstract void doMockSend(ChannelSendContext ctx, boolean success);
}