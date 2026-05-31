package com.duanruixin.pulse.app.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.duanruixin.pulse.app.dto.TenantCreateDTO;
import com.duanruixin.pulse.app.entity.Tenant;
import com.duanruixin.pulse.app.mapper.TenantMapper;
import com.duanruixin.pulse.app.service.TenantService;
import com.duanruixin.pulse.common.exception.BusinessException;
import com.duanruixin.pulse.common.result.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TenantServiceImpl
        extends ServiceImpl<TenantMapper, Tenant>
        implements TenantService {

    @Override
    public Tenant createTenant(TenantCreateDTO dto) {
        // 1. 检查 tenant_code 是否已存在
        Long count = this.lambdaQuery()
                .eq(Tenant::getTenantCode, dto.getTenantCode())
                .count();
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "租户编码已存在");
        }

        // 2. 构建实体并保存
        Tenant tenant = new Tenant();
        tenant.setTenantName(dto.getTenantName());
        tenant.setTenantCode(dto.getTenantCode());
        tenant.setContactName(dto.getContactName());
        tenant.setContactEmail(dto.getContactEmail());
        tenant.setContactPhone(dto.getContactPhone());
        tenant.setStatus(1);

        this.save(tenant);   // MP 提供的方法,内部调 insert,自动填充 createTime/updateTime
        log.info("租户创建成功: id={}, code={}", tenant.getId(), tenant.getTenantCode());
        return tenant;
    }
}