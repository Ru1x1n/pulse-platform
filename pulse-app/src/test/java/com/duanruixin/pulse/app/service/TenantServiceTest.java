package com.duanruixin.pulse.app.service;

import com.duanruixin.pulse.app.dto.TenantCreateDTO;
import com.duanruixin.pulse.app.entity.Tenant;
import com.duanruixin.pulse.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional   // 测试方法自动回滚,不污染数据库
class TenantServiceTest {

    @Autowired
    private TenantService tenantService;

    @Test
    void should_create_tenant_successfully() {
        TenantCreateDTO dto = new TenantCreateDTO();
        dto.setTenantName("单元测试租户");
        dto.setTenantCode("UT_" + UUID.randomUUID().toString().substring(0, 8));
        dto.setContactEmail("test@example.com");

        Tenant tenant = tenantService.createTenant(dto);

        assertNotNull(tenant.getId());
        assertEquals(1, tenant.getStatus());
        assertNotNull(tenant.getCreateTime());
    }

    @Test
    void should_reject_duplicate_tenant_code() {
        String code = "UT_DUP_" + UUID.randomUUID().toString().substring(0, 6);

        TenantCreateDTO dto1 = new TenantCreateDTO();
        dto1.setTenantName("租户1");
        dto1.setTenantCode(code);
        tenantService.createTenant(dto1);

        TenantCreateDTO dto2 = new TenantCreateDTO();
        dto2.setTenantName("租户2");
        dto2.setTenantCode(code);   // 同样的 code

        BusinessException ex = assertThrows(BusinessException.class,
                () -> tenantService.createTenant(dto2));
        assertTrue(ex.getMessage().contains("已存在"));
    }
}