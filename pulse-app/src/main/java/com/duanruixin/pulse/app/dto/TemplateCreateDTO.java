package com.duanruixin.pulse.app.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class TemplateCreateDTO {

    @NotBlank(message = "模板编码不能为空")
    @Size(max = 50)
    private String templateCode;

    @NotNull(message = "应用ID不能为空")
    private Long appId;

    @NotBlank(message = "模板名称不能为空")
    @Size(max = 100)
    private String templateName;

    @NotNull(message = "渠道类型不能为空")
    private Integer channelType;

    @NotBlank(message = "模板内容不能为空")
    private String content;
}