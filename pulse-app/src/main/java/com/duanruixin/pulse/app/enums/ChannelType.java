package com.duanruixin.pulse.app.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 渠道类型(对齐 t_channel_config.channel_type / t_template.channel_type)
 * 1-短信 2-邮件 3-App推送 4-微信公众号
 */
@Getter
@AllArgsConstructor
public enum ChannelType {

    SMS(1, "短信"),
    EMAIL(2, "邮件"),
    PUSH(3, "App推送"),
    WECHAT(4, "微信公众号");

    private final Integer code;
    private final String desc;

    public static ChannelType of(Integer code) {
        for (ChannelType t : values()) {
            if (t.code.equals(code)) {
                return t;
            }
        }
        return null;
    }
}