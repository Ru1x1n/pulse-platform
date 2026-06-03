package com.duanruixin.pulse.app.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.duanruixin.pulse.app.dto.AppCreateDTO;
import com.duanruixin.pulse.app.entity.App;

import java.util.List;

public interface AppService extends IService<App> {

    /**
     * 创建应用,自动生成 app_key 和 app_secret
     */
    App createApp(AppCreateDTO dto);

    /**
     * 查询租户下所有应用
     */
    List<App> listByTenantId(Long tenantId);
    App getByAppKeyCached(String appKey);
    App getByIdCached(Long appId);
}
