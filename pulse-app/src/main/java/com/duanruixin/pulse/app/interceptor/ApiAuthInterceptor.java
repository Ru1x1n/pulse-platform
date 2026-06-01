package com.duanruixin.pulse.app.interceptor;

import com.duanruixin.pulse.app.entity.App;
import com.duanruixin.pulse.app.service.AppService;
import com.duanruixin.pulse.common.exception.BusinessException;
import com.duanruixin.pulse.common.result.ErrorCode;
import com.duanruixin.pulse.common.util.SignUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.HashMap;
import java.util.Map;

/**
 * API 鉴权拦截器
 * 校验业务方调用接口时的 app_key + timestamp + sign
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApiAuthInterceptor implements HandlerInterceptor {

    private final AppService appService;

    /** 时间戳允许的偏差(毫秒)5 分钟 */
    private static final long TIMESTAMP_TOLERANCE = 5 * 60 * 1000L;

    /** Request attribute key,用于把 appId 传给后续业务代码 */
    public static final String ATTR_APP_ID = "X-App-Id";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String appKey = request.getHeader("X-App-Key");
        String timestamp = request.getHeader("X-Timestamp");
        String sign = request.getHeader("X-Sign");

        // 1. 三个 Header 不能为空
        if (appKey == null || timestamp == null || sign == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "缺少鉴权 Header");
        }

        // 2. 时间戳防重放(5 分钟内)
        long ts;
        try {
            ts = Long.parseLong(timestamp);
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.TIMESTAMP_EXPIRED, "时间戳格式错误");
        }
        long now = System.currentTimeMillis();
        if (Math.abs(now - ts) > TIMESTAMP_TOLERANCE) {
            log.warn("时间戳过期: appKey={}, ts={}, now={}", appKey, ts, now);
            throw new BusinessException(ErrorCode.TIMESTAMP_EXPIRED, "请求时间戳过期");
        }

        // 3. 根据 appKey 查应用(查 secret)
        App app = appService.getByAppKeyCached(appKey);
        if (app == null) {
            throw new BusinessException(ErrorCode.API_KEY_INVALID, "App Key 无效或已停用");
        }

        // 4. 验证签名
        // 参与签名的参数:appKey + timestamp(可以再加业务参数,这里简化)
        Map<String, String> params = new HashMap<>();
        params.put("appKey", appKey);
        params.put("timestamp", timestamp);
        params.put("sign", sign);

        if (!SignUtil.verify(params, app.getAppSecret())) {
            log.warn("签名校验失败: appKey={}", appKey);
            throw new BusinessException(ErrorCode.SIGN_INVALID);
        }

        // 5. 把 appId 放到 request,供业务代码使用
        request.setAttribute(ATTR_APP_ID, app.getId());
        log.debug("API 鉴权通过: appKey={}, appId={}", appKey, app.getId());

        return true;
    }
}