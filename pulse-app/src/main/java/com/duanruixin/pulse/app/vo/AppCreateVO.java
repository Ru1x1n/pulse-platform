package com.duanruixin.pulse.app.vo;

import lombok.Data;

@Data
public class AppCreateVO {
    private Long id;
    private String appKey;
    private String appSecret;   // 只在创建时返回一次,后续不暴露
    private String appName;
}