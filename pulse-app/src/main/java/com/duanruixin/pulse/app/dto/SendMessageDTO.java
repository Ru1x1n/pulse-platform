package com.duanruixin.pulse.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class SendMessageDTO {

    @NotBlank(message = "模板编码不能为空")
    private String templateCode;

    @NotEmpty(message = "接收者不能为空")
    private List<String> receivers;

    private Map<String, String> variables;
}