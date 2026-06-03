package com.duanruixin.pulse.app.controller;

import com.duanruixin.pulse.app.dto.SendMessageDTO;
import com.duanruixin.pulse.app.entity.Template;
import com.duanruixin.pulse.app.interceptor.ApiAuthInterceptor;
import com.duanruixin.pulse.app.service.TemplateRenderer;
import com.duanruixin.pulse.app.service.TemplateService;
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

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/external/v1")
@RequiredArgsConstructor
public class ExternalSendController {

    private final QuotaService quotaService;
    private final TemplateService templateService;
    private final TemplateRenderer templateRenderer;

    private final SnowflakeIdGenerator snowflake = new SnowflakeIdGenerator(1, 1);

    @PostMapping("/send")
    public Result<Map<String, Object>> send(
            HttpServletRequest request,
            @Valid @RequestBody SendMessageDTO dto) {

        Long appId = (Long) request.getAttribute(ApiAuthInterceptor.ATTR_APP_ID);

        // 1. 扣配额
        quotaService.deductQuota(appId);

        // 2. 查模板
        Template template = templateService.getByCodeCached(appId, dto.getTemplateCode());
        if (template == null) {
            throw new BusinessException(ErrorCode.TEMPLATE_NOT_FOUND);
        }

        // 3. 渲染模板
        String renderedContent = templateRenderer.render(template.getContent(), dto.getVariables());

        // 4. 生成 messageId(Day 6 会真发 MQ,现在只做日志)
        long messageId = snowflake.nextId();
        log.info("接收消息: appId={}, messageId={}, template={}, receivers={}, content={}",
                appId, messageId, dto.getTemplateCode(), dto.getReceivers(), renderedContent);

        // 5. 返回
        Map<String, Object> data = new HashMap<>();
        data.put("messageId", "MSG_" + messageId);
        data.put("status", "queued");
        data.put("renderedContent", renderedContent);  // 预览用,生产可去掉
        return Result.success(data);
    }
}