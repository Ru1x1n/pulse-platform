package com.duanruixin.pulse.common.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 错误码枚举
 * 规范:
 *   200xx: 通用
 *   400xx: 参数/认证错误
 *   500xx: 服务器内部错误
 *   600xx: 业务错误
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {

    SUCCESS(200, "OK"),

    PARAM_INVALID(40001, "参数无效"),
    UNAUTHORIZED(40101, "未授权"),
    API_KEY_INVALID(40102, "API Key 无效"),
    SIGN_INVALID(40103, "签名校验失败"),
    TIMESTAMP_EXPIRED(40104, "请求时间戳过期"),
    FORBIDDEN(40301, "权限不足"),
    NOT_FOUND(40401, "资源不存在"),

    SERVER_ERROR(50001, "服务器内部错误"),
    SERVICE_UNAVAILABLE(50301, "服务不可用"),

    APP_NOT_FOUND(60001, "应用不存在"),
    APP_DISABLED(60002, "应用已停用"),
    QUOTA_EXCEEDED(60003, "配额已用尽"),
    TEMPLATE_NOT_FOUND(60101, "模板不存在"),
    SENSITIVE_WORD_DETECTED(60201, "内容包含敏感词"),
    USER_BLOCKED(60202, "用户在黑名单"),
    RATE_LIMIT(60203, "发送过于频繁"),
    CHANNEL_UNAVAILABLE(60301, "渠道不可用");

    private final Integer code;
    private final String message;
}