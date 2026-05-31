package com.duanruixin.pulse.app.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.duanruixin.pulse.app.dto.AppCreateDTO;
import com.duanruixin.pulse.app.entity.App;
import com.duanruixin.pulse.app.entity.Tenant;
import com.duanruixin.pulse.app.mapper.AppMapper;
import com.duanruixin.pulse.app.service.AppService;
import com.duanruixin.pulse.app.service.TenantService;
import com.duanruixin.pulse.common.exception.BusinessException;
import com.duanruixin.pulse.common.result.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor   // Lombok,自动生成全参构造,实现构造方法注入
public class AppServiceImpl
        extends ServiceImpl<AppMapper, App>
        implements AppService {

    private final TenantService tenantService;

    @Override
    public App createApp(AppCreateDTO dto) {
        // 1. 校验租户存在
        Tenant tenant = tenantService.getById(dto.getTenantId());
        if (tenant == null) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "租户不存在");
        }
        if (tenant.getStatus() == 0) {
            throw new BusinessException(ErrorCode.APP_DISABLED, "租户已停用");
        }

        // 2. 生成 app_key 和 app_secret
        String appKey = "pulse_" + RandomUtil.randomString(32);
        String appSecret = RandomUtil.randomString(64);

        // 3. 构建实体保存
        App app = new App();
        app.setTenantId(dto.getTenantId());
        app.setAppName(dto.getAppName());
        app.setAppKey(appKey);
        app.setAppSecret(appSecret);
        app.setDailyQuota(dto.getDailyQuota() == null ? 10000 : dto.getDailyQuota());
        app.setStatus(1);

        this.save(app);
        log.info("应用创建成功: id={}, name={}, key={}", app.getId(), app.getAppName(), app.getAppKey());
        return app;
    }

    @Override
    public List<App> listByTenantId(Long tenantId) {
        return this.lambdaQuery()
                .eq(App::getTenantId, tenantId)
                .eq(App::getStatus, 1)
                .orderByDesc(App::getCreateTime)
                .list();
    }
}