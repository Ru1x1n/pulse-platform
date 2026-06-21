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

    /** 重试延时级别映射:第1次重试用level1(1s),第2次level2(5s)...
     *  RocketMQ 内置级别: 1s 5s 10s 30s 1m 2m ... 对应 1 2 3 4 5 6 ...
     *  取 1s/5s/30s/2m/10m → level 1/2/4/6/14 */
    private static final int[] RETRY_DELAY_LEVELS = {1, 2, 4, 6, 14};

    /**
     * 投递重试任务(延时消息)
     * @param task 已把 retryCount 设为"本次是第几次重试(1-based)"
     */
    public void sendRetryTask(MessageTask task) {
        int retryCount = task.getRetryCount();   // 1,2,3,4,5
        // 取对应延时级别(retryCount 从1开始,数组下标从0)
        int delayLevel = RETRY_DELAY_LEVELS[retryCount - 1];

        Message<MessageTask> message = MessageBuilder.withPayload(task)
                .setHeader(RocketMQHeaders.KEYS, task.getMessageId())
                .build();

        // syncSend 带延时级别的重载:timeout=3000ms, delayLevel
        SendResult result = rocketMQTemplate.syncSend(
                TOPIC_MESSAGE_SEND, message, 3000, delayLevel);

        log.info("重试消息已投递(延时): messageId={}, 第{}次重试, delayLevel={}, brokerMsgId={}",
                task.getMessageId(), retryCount, delayLevel, result.getMsgId());
    }
}