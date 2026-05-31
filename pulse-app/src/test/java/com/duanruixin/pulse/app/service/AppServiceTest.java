package com.duanruixin.pulse.app.service;

import com.duanruixin.pulse.app.dto.AppCreateDTO;
import com.duanruixin.pulse.app.dto.TenantCreateDTO;
import com.duanruixin.pulse.app.entity.App;
import com.duanruixin.pulse.app.entity.Tenant;
import com.duanruixin.pulse.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class AppServiceTest {

    @Autowired
    private TenantService tenantService;

    @Autowired
    private AppService appService;

    private Long createTestTenant() {
        TenantCreateDTO dto = new TenantCreateDTO();
        dto.setTenantName("测试租户");
        dto.setTenantCode("UT_" + UUID.randomUUID().toString().substring(0, 8));
        return tenantService.createTenant(dto).getId();
    }

    @Test
    void should_create_app_with_key_and_secret() {
        Long tenantId = createTestTenant();
        AppCreateDTO dto = new AppCreateDTO();
        dto.setTenantId(tenantId);
        dto.setAppName("测试应用");

        App app = appService.createApp(dto);

        assertNotNull(app.getId());
        assertNotNull(app.getAppKey());
        assertNotNull(app.getAppSecret());
        assertTrue(app.getAppKey().startsWith("pulse_"));
        assertEquals(38, app.getAppKey().length());   // pulse_ + 32 字符
        assertEquals(64, app.getAppSecret().length());
        assertEquals(10000, app.getDailyQuota());   // 默认配额
    }

    @Test
    void should_reject_app_with_invalid_tenant() {
        AppCreateDTO dto = new AppCreateDTO();
        dto.setTenantId(99999L);   // 不存在的租户
        dto.setAppName("测试");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> appService.createApp(dto));
        assertTrue(ex.getMessage().contains("租户不存在"));
    }

    @Test
    void should_generate_unique_app_keys() {
        Long tenantId = createTestTenant();
        AppCreateDTO dto1 = new AppCreateDTO();
        dto1.setTenantId(tenantId);
        dto1.setAppName("应用1");

        AppCreateDTO dto2 = new AppCreateDTO();
        dto2.setTenantId(tenantId);
        dto2.setAppName("应用2");

        App app1 = appService.createApp(dto1);
        App app2 = appService.createApp(dto2);

        assertNotEquals(app1.getAppKey(), app2.getAppKey());
        assertNotEquals(app1.getAppSecret(), app2.getAppSecret());
    }
}