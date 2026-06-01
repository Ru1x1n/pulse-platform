package com.duanruixin.pulse.app.controller;

import com.duanruixin.pulse.app.interceptor.ApiAuthInterceptor;
import com.duanruixin.pulse.app.service.quota.QuotaService;
import com.duanruixin.pulse.common.result.Result;
import com.duanruixin.pulse.common.util.SnowflakeIdGenerator;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/external/v1")
@RequiredArgsConstructor
public class ExternalSendController {

    private final QuotaService quotaService;
    private final SnowflakeIdGenerator snowflake = new SnowflakeIdGenerator(1, 1);

    /**
     * 模拟发送消息(Day 4 占位,Day 6 会做真的发送)
     */
    @PostMapping("/send")
    public Result<Map<String, Object>> send(HttpServletRequest request) {
        // 1. 从 request 拿 appId(拦截器已经放进来了)
        Long appId = (Long) request.getAttribute(ApiAuthInterceptor.ATTR_APP_ID);

        // 2. 扣减配额
        quotaService.deductQuota(appId);

        // 3. 生成 messageId(用上 Day 2 的雪花算法)
        long messageId = snowflake.nextId();

        // 4. 返回(后面 Day 6 会真的把消息扔 MQ)
        Map<String, Object> data = new HashMap<>();
        data.put("messageId", "MSG_" + messageId);
        data.put("status", "queued");
        data.put("usedQuota", quotaService.getUsedQuota(appId));

        log.info("接收消息: appId={}, messageId={}", appId, messageId);
        return Result.success(data);
    }
}