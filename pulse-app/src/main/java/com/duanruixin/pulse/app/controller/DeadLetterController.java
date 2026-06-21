package com.duanruixin.pulse.app.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.duanruixin.pulse.app.entity.DeadLetter;
import com.duanruixin.pulse.app.mapper.DeadLetterMapper;
import com.duanruixin.pulse.app.mq.MessageProducer;
import com.duanruixin.pulse.app.mq.MessageTask;
import com.duanruixin.pulse.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 死信管理接口(后台,无鉴权)
 */
@RestController
@RequestMapping("/api/v1/dead-letter")
@RequiredArgsConstructor
public class DeadLetterController {

    private final DeadLetterMapper deadLetterMapper;
    private final MessageProducer messageProducer;

    /** 死信列表 */
    @GetMapping("/list")
    public Result<List<DeadLetter>> list() {
        return Result.success(deadLetterMapper.selectList(
                Wrappers.<DeadLetter>lambdaQuery().orderByDesc(DeadLetter::getId)));
    }

    /** 手动重发某条死信(重新投回 MQ,retryCount 归0,重走完整流程) */
    @PostMapping("/resend/{messageId}")
    public Result<String> resend(@PathVariable String messageId) {
        DeadLetter dl = deadLetterMapper.selectOne(
                Wrappers.<DeadLetter>lambdaQuery().eq(DeadLetter::getMessageId, messageId));
        if (dl == null) {
            return Result.success("死信不存在: " + messageId);
        }
        MessageTask task = new MessageTask();
        task.setMessageId(dl.getMessageId());
        task.setAppId(dl.getAppId());
        task.setTemplateCode(dl.getTemplateCode());
        task.setReceiver(dl.getReceiver());
        task.setContent(dl.getContent());
        task.setEnqueueTime(System.currentTimeMillis());
        task.setRetryCount(0);                 // 归0,重走正常流程
        messageProducer.sendTask(task);        // 注意:用普通 sendTask 不是延时

        return Result.success("已重新投递: " + messageId);
    }
}