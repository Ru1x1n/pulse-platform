package com.duanruixin.pulse.app.mq;

import com.duanruixin.pulse.app.entity.ChannelConfig;
import com.duanruixin.pulse.app.entity.Template;
import com.duanruixin.pulse.app.enums.MessageStatus;
import com.duanruixin.pulse.app.service.TemplateService;
import com.duanruixin.pulse.app.service.channel.ChannelHealthService;
import com.duanruixin.pulse.app.service.channel.ChannelRouter;
import com.duanruixin.pulse.app.service.channel.handler.ChannelFactory;
import com.duanruixin.pulse.app.service.channel.handler.ChannelSendContext;
import com.duanruixin.pulse.app.service.message.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = MessageProducer.TOPIC_MESSAGE_SEND,
        consumerGroup = "pulse_message_consumer_group"
)
public class MessageConsumer implements RocketMQListener<MessageTask> {

    private final MessageService messageService;
    private final TemplateService templateService;
    private final ChannelRouter channelRouter;
    private final ChannelFactory channelFactory;        // Day12: 替换 MockChannelSender
    private final ChannelHealthService channelHealthService;

    @Override
    public void onMessage(MessageTask task) {
        String messageId = task.getMessageId();
        log.info("【MQ消费】收到任务: messageId={}, appId={}, receiver={}",
                messageId, task.getAppId(), task.getReceiver());

        // 待发 → 发送中
        messageService.updateStatus(messageId,
                MessageStatus.PENDING, MessageStatus.SENDING, "开始处理");

        try {
            // 1. 查模板拿 channelType
            Template template = templateService.getByCodeCached(
                    task.getAppId(), task.getTemplateCode());
            Integer channelType = template.getChannelType();

            // 2. 智能路由:选 provider(Day11)
            ChannelConfig channel = channelRouter.route(task.getAppId(), channelType);

            // 3. 策略模式发送:按渠道类型分发到对应 Handler(Day12)
            ChannelSendContext ctx = new ChannelSendContext(channel, task);
            boolean success = channelFactory.get(channelType).send(ctx);

            // 4. 埋点:记录成败到健康度 ZSet(Day11)
            channelHealthService.record(channelType, channel.getProvider(), success);

            // 5. 推进状态(重试/死信留 Day15)
            if (success) {
                messageService.updateStatus(messageId,
                        MessageStatus.SENDING, MessageStatus.SUCCESS,
                        "发送成功(" + channel.getProvider() + ")");
            } else {
                messageService.updateStatus(messageId,
                        MessageStatus.SENDING, MessageStatus.FAILED,
                        "发送失败(" + channel.getProvider() + ")");
            }
        } catch (Exception e) {
            log.error("【MQ消费】处理失败: messageId={}, err={}", messageId, e.getMessage(), e);
            messageService.updateStatus(messageId,
                    MessageStatus.SENDING, MessageStatus.FAILED,
                    "处理异常:" + e.getMessage());
        }
    }
}