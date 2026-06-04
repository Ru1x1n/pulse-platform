package com.duanruixin.pulse.app.mq;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 消息发送消费者(Day6 占位版,仅打日志)
 * TODO Day14: 频控 → 敏感词 → 路由 → 渠道发送 → 写 t_message_track
 */
@Slf4j
@Component
@RocketMQMessageListener(
        topic = MessageProducer.TOPIC_MESSAGE_SEND,
        consumerGroup = "pulse_message_consumer_group"
)
public class MessageConsumer implements RocketMQListener<MessageTask> {

    @Override
    public void onMessage(MessageTask task) {
        log.info("【MQ消费占位】收到发送任务: messageId={}, appId={}, templateCode={}, receiver={}, variables={}",
                task.getMessageId(), task.getAppId(), task.getTemplateCode(),
                task.getReceiver(), task.getContent());
    }
}



//占位用的。