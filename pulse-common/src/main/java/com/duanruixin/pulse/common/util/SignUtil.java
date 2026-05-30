package com.duanruixin.pulse.common.util;

import lombok.extern.slf4j.Slf4j;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;

/**
 * HMAC-SHA256 签名工具
 * 用于 API 鉴权:防止请求被篡改,验证调用方身份
 */
@Slf4j
public class SignUtil {

    private static final String ALGORITHM = "HmacSHA256";

    /**
     * 生成签名
     *
     * @param params 参数 Map(会按 key 字典序排序)
     * @param secret 签名密钥
     * @return 签名字符串(16 进制小写)
     */
    public static String sign(Map<String, String> params, String secret) {
        // 1. 按 key 字典序排序,拼接成 k1=v1&k2=v2&... 形式
        String content = buildSignContent(params);
        // 2. HMAC-SHA256 签名
        return hmacSha256(content, secret);
    }

    /**
     * 校验签名
     *
     * @param params 参数 Map(包含传入的 sign 字段)
     * @param secret 签名密钥
     * @return 是否合法
     */
    public static boolean verify(Map<String, String> params, String secret) {
        if (params == null || !params.containsKey("sign")) {
            return false;
        }
        // 拿出对方传的 sign,从 Map 中移除(排序时不包含 sign 自己)
        String receivedSign = params.get("sign");
        Map<String, String> paramsCopy = new TreeMap<>(params);
        paramsCopy.remove("sign");

        String computedSign = sign(paramsCopy, secret);
        return computedSign.equalsIgnoreCase(receivedSign);
    }

    private static String buildSignContent(Map<String, String> params) {
        // TreeMap 按 key 字典序自动排序
        TreeMap<String, String> sorted = new TreeMap<>(params);
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : sorted.entrySet()) {
            // 跳过空值
            if (entry.getValue() == null || entry.getValue().isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append("&");
            }
            sb.append(entry.getKey()).append("=").append(entry.getValue());
        }
        return sb.toString();
    }

    private static String hmacSha256(String content, String secret) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM);
            mac.init(keySpec);
            byte[] bytes = mac.doFinal(content.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(bytes);
        } catch (Exception e) {
            log.error("签名生成失败", e);
            throw new RuntimeException("签名生成失败", e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }


}