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

    /** 最大重试次数,超过进死信 */
    private static final int MAX_RETRY = 5;

    private final MessageService messageService;
    private final TemplateService templateService;
    private final ChannelRouter channelRouter;
    private final ChannelFactory channelFactory;
    private final ChannelHealthService channelHealthService;
    private final MessageProducer messageProducer;       // Day15: 投延时重试

    @Override
    public void onMessage(MessageTask task) {
        String messageId = task.getMessageId();
        int retryCount = task.getRetryCount() == null ? 0 : task.getRetryCount();
        log.info("【MQ消费】收到任务: messageId={}, receiver={}, 第{}次投递(retryCount={})",
                messageId, task.getReceiver(), retryCount, retryCount);

        // 开头推进到 SENDING:首次从 PENDING 来,重试从 FAILED 来(状态机难点)
        MessageStatus fromStatus = (retryCount == 0)
                ? MessageStatus.PENDING : MessageStatus.FAILED;
        boolean advanced = messageService.updateStatus(messageId,
                fromStatus, MessageStatus.SENDING,
                retryCount == 0 ? "开始处理" : "第" + retryCount + "次重试");
        if (!advanced) {
            // CAS 失败:状态非预期(可能重复消费),直接放弃,不重复发送
            log.warn("【MQ消费】状态推进失败,跳过: messageId={}, 期望from={}", messageId, fromStatus);
            return;
        }

        Integer channelType = null;
        String lastError;
        try {
            // 1. 查模板拿 channelType
            Template template = templateService.getByCodeCached(task.getAppId(), task.getTemplateCode());
            channelType = template.getChannelType();

            // 2. 路由选 provider
            ChannelConfig channel = channelRouter.route(task.getAppId(), channelType);

            // 3. 策略发送
            ChannelSendContext ctx = new ChannelSendContext(channel, task);
            boolean success = channelFactory.get(channelType).send(ctx);

            // 4. 埋点
            channelHealthService.record(channelType, channel.getProvider(), success);

            if (success) {
                messageService.updateStatus(messageId, MessageStatus.SENDING, MessageStatus.SUCCESS,
                        "发送成功(" + channel.getProvider() + ")");
                return;
            }
            // 发送失败,进入重试/死信处理
            lastError = "发送失败(" + channel.getProvider() + ")";
        } catch (Exception e) {
            log.error("【MQ消费】处理异常: messageId={}, err={}", messageId, e.getMessage(), e);
            lastError = "处理异常:" + e.getMessage();
        }

        // ===== 失败统一处理:能重试则延时重投,否则进死信 =====
        handleFailure(task, channelType, lastError, retryCount);
    }

    /**
     * 失败处理:retryCount<MAX 投延时重试,否则进死信
     */
    private void handleFailure(MessageTask task, Integer channelType, String lastError, int retryCount) {
        String messageId = task.getMessageId();

        // 先把当前这次"发送中"落回 FAILED(下次重试的 CAS 前置态)
        messageService.updateStatus(messageId, MessageStatus.SENDING, MessageStatus.FAILED, lastError);

        if (retryCount < MAX_RETRY) {
            int nextRetry = retryCount + 1;
            task.setRetryCount(nextRetry);
            messageService.updateRetryCount(messageId, nextRetry);   // 库里 retry_count 同步
            messageProducer.sendRetryTask(task);                     // 投延时消息
            log.warn("【重试】messageId={} 安排第{}次重试", messageId, nextRetry);
        } else {
            // 重试耗尽 → 死信
            messageService.moveToDeadLetter(task,
                    channelType == null ? 0 : channelType, lastError);
        }
    }
}