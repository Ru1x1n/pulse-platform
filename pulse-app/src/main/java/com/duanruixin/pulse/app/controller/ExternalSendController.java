package com.duanruixin.pulse.app.controller;

import com.duanruixin.pulse.app.dto.SendMessageDTO;
import com.duanruixin.pulse.app.entity.Template;
import com.duanruixin.pulse.app.interceptor.ApiAuthInterceptor;
import com.duanruixin.pulse.app.mq.MessageProducer;
import com.duanruixin.pulse.app.mq.MessageTask;
import com.duanruixin.pulse.app.service.TemplateRenderer;
import com.duanruixin.pulse.app.service.TemplateService;
import com.duanruixin.pulse.app.service.audit.AuditService;
import com.duanruixin.pulse.app.service.quota.QuotaService;
import com.duanruixin.pulse.common.exception.BusinessException;
import com.duanruixin.pulse.common.result.ErrorCode;
import com.duanruixin.pulse.common.result.Result;
import com.duanruixin.pulse.common.util.SnowflakeIdGenerator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/external/v1")
@RequiredArgsConstructor
public class ExternalSendController {

    private final QuotaService quotaService;
    private final TemplateService templateService;
    private final TemplateRenderer templateRenderer;
    private final MessageProducer messageProducer;
    private final AuditService auditService;
    private final SnowflakeIdGenerator snowflake = new SnowflakeIdGenerator(1, 1);

    @PostMapping("/send")
    public Result<Map<String, Object>> send(
            HttpServletRequest request,
            @Valid @RequestBody SendMessageDTO dto) {

        Long appId = (Long) request.getAttribute(ApiAuthInterceptor.ATTR_APP_ID);

        // 1. 扣配额(Day6 仍按"每次请求扣 1";按接收者数批量扣减放 Day16 防超卖一起做)
        quotaService.deductQuota(appId);

        // 2. 查模板(同步校验:模板不存在立即报错,不进 MQ)
        Template template = templateService.getByCodeCached(appId, dto.getTemplateCode());
        if (template == null) {
            throw new BusinessException(ErrorCode.TEMPLATE_NOT_FOUND);
        }

        // 3. 渲染模板(variables 全接收者共用,渲染一次;缺变量同步抛错)
        String renderedContent = templateRenderer.render(template.getContent(), dto.getVariables());
        auditService.check(renderedContent);


        // 4. 每个接收者生成一个 messageId,组任务投 MQ(发送动作异步化)
        List<String> messageIds = new ArrayList<>();
        for (String receiver : dto.getReceivers()) {
            String messageId = "MSG_" + snowflake.nextId();

            MessageTask task = new MessageTask();
            task.setMessageId(messageId);
            task.setAppId(appId);
            task.setTemplateCode(dto.getTemplateCode());
            task.setReceiver(receiver);
            task.setContent(renderedContent);
            task.setEnqueueTime(System.currentTimeMillis());

            messageProducer.sendTask(task);
            messageIds.add(messageId);
        }

        log.info("发送任务已受理: appId={}, template={}, 接收者数={}, messageIds={}",
                appId, dto.getTemplateCode(), dto.getReceivers().size(), messageIds);

        // 5. 立即返回受理结果(不等真实发送)
        Map<String, Object> data = new HashMap<>();
        data.put("status", "accepted");
        data.put("count", messageIds.size());
        data.put("messageIds", messageIds);
        data.put("renderedContent", renderedContent); // 预览用,生产可去掉
        return Result.success(data);
    }
}