package com.duanruixin.pulse.app.service.freq;

import com.duanruixin.pulse.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class FreqLimitServiceTest {

    @Autowired
    private FreqLimitService freqLimitService;

    @Test
    void 同手机号一分钟内第二条被限() {
        Long appId = 999L;
        // 用随机手机号避免和历史数据/其它测试冲突
        String phone = "138" + System.currentTimeMillis() % 100000000;

        // 第 1 条通过
        assertDoesNotThrow(() -> freqLimitService.checkAndRecord(appId, phone));

        // 第 2 条(1 分钟内)被限
        BusinessException ex = assertThrows(BusinessException.class,
                () -> freqLimitService.checkAndRecord(appId, phone));
        assertTrue(ex.getMessage().contains("每分钟"));
    }

    @Test
    void 不同手机号互不影响() {
        Long appId = 999L;
        String phoneA = "138" + System.currentTimeMillis() % 100000000;
        String phoneB = "139" + System.currentTimeMillis() % 100000000;

        assertDoesNotThrow(() -> freqLimitService.checkAndRecord(appId, phoneA));
        // B 是新号,第一条应通过,不受 A 影响
        assertDoesNotThrow(() -> freqLimitService.checkAndRecord(appId, phoneB));
    }
}