package com.duanruixin.pulse.common.util;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SignUtilTest {

    private static final String SECRET = "test-secret-key";

    @Test
    void should_generate_consistent_sign_for_same_params() {
        Map<String, String> params = buildParams();
        String sign1 = SignUtil.sign(params, SECRET);
        String sign2 = SignUtil.sign(params, SECRET);
        assertEquals(sign1, sign2, "相同参数应该生成相同签名");
    }

    @Test
    void should_verify_valid_sign() {
        Map<String, String> params = buildParams();
        String sign = SignUtil.sign(params, SECRET);
        params.put("sign", sign);
        assertTrue(SignUtil.verify(params, SECRET));
    }

    @Test
    void should_reject_tampered_params() {
        Map<String, String> params = buildParams();
        String sign = SignUtil.sign(params, SECRET);
        params.put("sign", sign);
        params.put("appKey", "hacker");  // 篡改
        assertFalse(SignUtil.verify(params, SECRET));
    }

    @Test
    void should_reject_wrong_secret() {
        Map<String, String> params = buildParams();
        String sign = SignUtil.sign(params, SECRET);
        params.put("sign", sign);
        assertFalse(SignUtil.verify(params, "wrong-secret"));
    }

    private Map<String, String> buildParams() {
        Map<String, String> params = new HashMap<>();
        params.put("appKey", "pulse-app");
        params.put("timestamp", "1716998765432");
        params.put("nonce", "abc123");
        return params;
    }
}