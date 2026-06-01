package com.duanruixin.pulse.app;

import com.duanruixin.pulse.common.util.SignUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * 临时签名生成工具
 * 用于 Postman 调试时生成 X-Sign 请求头
 * Day 4 测试完可删除,或保留作为 Day 5+ 后续接口调试用
 */
public class SignGenerator {

    public static void main(String[] args) {
        // ============================================
        // ⚠️ 用你创建应用时返回的真实 key 和 secret
        // ============================================
        String appKey = "pulse_V8F368Yfkq4QJqsIE80Iz6nyMMFtsNUe";
        String appSecret = "fmDpGRoSkXIjiSUUtMbAVf8nHyFRD6pSFy6CtB0AYTslkwjs37ZEINArboRE2peP";

        // 当前时间戳
        long timestamp = System.currentTimeMillis();

        // 构造签名参数(必须和 ApiAuthInterceptor 里的参数一致)
        Map<String, String> params = new HashMap<>();
        params.put("appKey", appKey);
        params.put("timestamp", String.valueOf(timestamp));

        String sign = SignUtil.sign(params, appSecret);

        System.out.println("=========================================");
        System.out.println("Postman Headers (复制下面三行到 Postman):");
        System.out.println("=========================================");
        System.out.println("X-App-Key: " + appKey);
        System.out.println("X-Timestamp: " + timestamp);
        System.out.println("X-Sign: " + sign);
        System.out.println("=========================================");
    }
}