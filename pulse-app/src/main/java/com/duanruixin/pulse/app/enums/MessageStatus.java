package com.duanruixin.pulse.app.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MessageStatus {

    PENDING(0, "待发"),
    SENDING(1, "发送中"),
    SUCCESS(2, "成功"),
    FAILED(3, "失败"),
    RETURNED(4, "退回");

    private final Integer code;
    private final String desc;
}
