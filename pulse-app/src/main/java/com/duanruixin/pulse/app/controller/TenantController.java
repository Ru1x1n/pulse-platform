package com.duanruixin.pulse.app.controller;

import com.duanruixin.pulse.app.dto.TenantCreateDTO;
import com.duanruixin.pulse.app.entity.Tenant;
import com.duanruixin.pulse.app.service.TenantService;
import com.duanruixin.pulse.common.result.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/tenant")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;

    /**
     * 创建租户
     */
    @PostMapping
    public Result<Tenant> createTenant(@Valid @RequestBody TenantCreateDTO dto) {
        Tenant tenant = tenantService.createTenant(dto);
        return Result.success(tenant);
    }

    /**
     * 查询租户详情
     */
    @GetMapping("/{id}")
    public Result<Tenant> getTenant(@PathVariable("id") Long id) {   // ← 加 "id"
        Tenant tenant = tenantService.getById(id);
        return Result.success(tenant);
    }
}