package com.duanruixin.pulse.app.mq;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.rocketmq.spring.support.RocketMQHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

/**
 * 消息发送生产者:把发送任务投递到 MQ,接收端不阻塞
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageProducer {

    /** 发送任务主题 */
    public static final String TOPIC_MESSAGE_SEND = "pulse_message_send";

    private final RocketMQTemplate rocketMQTemplate;

    /**
     * 同步投递(到 MQ 是毫秒级,不等真实渠道发送)
     * keys 设为 messageId,方便后续按 messageId 检索消息
     */
    public void sendTask(MessageTask task) {
        Message<MessageTask> message = MessageBuilder.withPayload(task)
                .setHeader(RocketMQHeaders.KEYS, task.getMessageId())
                .build();

        SendResult result = rocketMQTemplate.syncSend(TOPIC_MESSAGE_SEND, message);
        log.info("消息入队成功: messageId={}, brokerMsgId={}, status={}",
                task.getMessageId(), result.getMsgId(), result.getSendStatus());
    }
}