package com.duanruixin.pulse.app.service.message;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.duanruixin.pulse.app.entity.Message;
import com.duanruixin.pulse.app.entity.MessageTrack;
import com.duanruixin.pulse.app.enums.MessageStatus;
import com.duanruixin.pulse.app.mapper.MessageMapper;
import com.duanruixin.pulse.app.mapper.MessageTrackMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageMapper messageMapper;
    private final MessageTrackMapper messageTrackMapper;

    /**
     * 落库一条待发消息 + 写初始轨迹
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveMessage(String messageId, Long appId, String templateCode,
                            String receiver, String content, Integer channelType) {
        Message msg = new Message();
        msg.setMessageId(messageId);
        msg.setAppId(appId);
        msg.setTemplateCode(templateCode);
        msg.setReceiver(receiver);
        msg.setContent(content);
        msg.setChannelType(channelType);
        msg.setStatus(MessageStatus.PENDING.getCode());
        msg.setRetryCount(0);
        messageMapper.insert(msg);

        recordTrack(messageId, null, MessageStatus.PENDING, "消息创建");
        log.debug("消息落库: messageId={}, status=待发", messageId);
    }

    /**
     * CAS 推进状态:仅当当前状态 == from 时才更新,并写轨迹
     * 返回 false 表示状态已变(可能被重复消费/并发),调用方按需处理
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean updateStatus(String messageId, MessageStatus from, MessageStatus to, String remark) {
        Message update = new Message();
        update.setStatus(to.getCode());

        int rows = messageMapper.update(update,
                Wrappers.<Message>lambdaUpdate()
                        .eq(Message::getMessageId, messageId)
                        .eq(Message::getStatus, from.getCode()));

        if (rows == 0) {
            log.warn("状态推进跳过(当前非预期前置态): messageId={}, 期望from={}, to={}",
                    messageId, from, to);
            return false;
        }
        recordTrack(messageId, from.getCode(), to, remark);
        log.debug("状态推进: messageId={}, {} -> {}", messageId, from.getDesc(), to.getDesc());
        return true;
    }

    private void recordTrack(String messageId, Integer fromStatus, MessageStatus to, String remark) {
        MessageTrack track = new MessageTrack();
        track.setMessageId(messageId);
        track.setFromStatus(fromStatus);
        track.setToStatus(to.getCode());
        track.setRemark(remark);
        messageTrackMapper.insert(track);
    }
}