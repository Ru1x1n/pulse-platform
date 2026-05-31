package com.duanruixin.pulse.app.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.duanruixin.pulse.app.dto.TenantCreateDTO;
import com.duanruixin.pulse.app.entity.Tenant;

public interface TenantService extends IService<Tenant> {

    /**
     * 创建租户
     */
    Tenant createTenant(TenantCreateDTO dto);
}