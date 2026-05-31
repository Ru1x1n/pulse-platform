package com.duanruixin.pulse.app.controller;

import com.duanruixin.pulse.app.dto.AppCreateDTO;
import com.duanruixin.pulse.app.entity.App;
import com.duanruixin.pulse.app.service.AppService;
import com.duanruixin.pulse.app.vo.AppCreateVO;
import com.duanruixin.pulse.common.result.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/app")
@RequiredArgsConstructor
public class AppController {

    private final AppService appService;

    /**
     * 创建应用(返回 app_key + app_secret,secret 只在此时显示一次)
     */
    @PostMapping
    public Result<AppCreateVO> createApp(@Valid @RequestBody AppCreateDTO dto) {
        App app = appService.createApp(dto);
        AppCreateVO vo = new AppCreateVO();
        BeanUtils.copyProperties(app, vo);
        return Result.success(vo);
    }

    /**
     * 查询应用详情(不返回 secret)
     */
    @GetMapping("/{id}")
    public Result<App> getApp(@PathVariable("id") Long id) {
        App app = appService.getById(id);
        if (app != null) {
            app.setAppSecret(null);   // 不暴露 secret
        }
        return Result.success(app);
    }

    /**
     * 查询租户下所有应用
     */
    @GetMapping("/by-tenant/{tenantId}")
    public Result<List<App>> listByTenant(@PathVariable("tenantId") Long tenantId) {
        List<App> apps = appService.listByTenantId(tenantId);
        // 列表也不暴露 secret
        apps.forEach(a -> a.setAppSecret(null));
        return Result.success(apps);
    }
}