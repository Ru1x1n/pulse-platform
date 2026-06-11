package com.duanruixin.pulse.app.mq;

import com.duanruixin.pulse.app.enums.MessageStatus;
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

    @Override
    public void onMessage(MessageTask task) {
        String messageId = task.getMessageId();
        log.info("【MQ消费】收到任务: messageId={}, appId={}, receiver={}",
                messageId, task.getAppId(), task.getReceiver());

        // 待发 → 发送中
        messageService.updateStatus(messageId,
                MessageStatus.PENDING, MessageStatus.SENDING, "开始处理");

        // TODO Day14: 这里换成真实渠道发送。Day10 先模拟成功
        messageService.updateStatus(messageId,
                MessageStatus.SENDING, MessageStatus.SUCCESS, "模拟发送成功");
    }
}