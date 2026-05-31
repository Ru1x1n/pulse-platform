package com.duanruixin.pulse.app.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TenantCreateDTO {

    @NotBlank(message = "租户名称不能为空")
    @Size(max = 100, message = "租户名称最长 100 字符")
    private String tenantName;

    @NotBlank(message = "租户编码不能为空")
    @Size(max = 50, message = "租户编码最长 50 字符")
    private String tenantCode;

    private String contactName;

    @Email(message = "邮箱格式不正确")
    private String contactEmail;

    private String contactPhone;
}