package com.duanruixin.pulse.app.service.channel.handler;

import com.duanruixin.pulse.app.enums.ChannelType;
import com.duanruixin.pulse.common.exception.BusinessException;
import com.duanruixin.pulse.common.result.ErrorCode;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 渠道策略工厂
 * Spring 自动注入所有 ChannelHandler 实现 → 启动时按 channelType 建索引
 * 新增渠道 = 新增一个 @Component 实现类,本工厂无需改动(开闭原则)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChannelFactory {

    /** Spring 自动把所有 ChannelHandler 实现类注入进来 */
    private final List<ChannelHandler> handlers;

    /** channelType → handler 索引 */
    private final Map<ChannelType, ChannelHandler> handlerMap = new EnumMap<>(ChannelType.class);

    @PostConstruct
    public void init() {
        for (ChannelHandler handler : handlers) {
            ChannelType type = handler.support();
            ChannelHandler exist = handlerMap.put(type, handler);
            if (exist != null) {
                // 同一渠道类型注册了两个 Handler,启动期就暴露,别等线上
                throw new IllegalStateException("渠道类型 " + type + " 注册了重复 Handler: "
                        + exist.getClass().getSimpleName() + " / " + handler.getClass().getSimpleName());
            }
        }
        log.info("【渠道工厂】已注册 {} 个渠道 Handler: {}", handlerMap.size(), handlerMap.keySet());
    }

    /**
     * 按渠道类型(数字 code)取 Handler
     */
    public ChannelHandler get(Integer channelTypeCode) {
        ChannelType type = ChannelType.of(channelTypeCode);
        if (type == null) {
            throw new BusinessException(ErrorCode.CHANNEL_UNAVAILABLE,
                    "未知渠道类型: " + channelTypeCode);
        }
        ChannelHandler handler = handlerMap.get(type);
        if (handler == null) {
            throw new BusinessException(ErrorCode.CHANNEL_UNAVAILABLE,
                    "渠道类型未注册 Handler: " + type);
        }
        return handler;
    }
}