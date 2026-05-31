package com.duanruixin.pulse.app.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AppCreateDTO {

    @NotNull(message = "租户 ID 不能为空")
    private Long tenantId;

    @NotBlank(message = "应用名称不能为空")
    private String appName;

    private Integer dailyQuota;
}