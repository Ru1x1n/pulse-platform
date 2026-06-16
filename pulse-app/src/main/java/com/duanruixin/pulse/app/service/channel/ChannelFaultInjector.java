package com.duanruixin.pulse.app.service.channel;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 故障注入器(仅测试用,Day13 真渠道接入后删除)
 * 给某 {channelType:provider} 设定失败率,MockChannelSender 据此模拟失败
 */
@Slf4j
@Component
public class ChannelFaultInjector {

    /** key = channelType:provider, value = 失败率 [0,1] */
    private final ConcurrentHashMap<String, Double> failRateMap = new ConcurrentHashMap<>();

    public void setFailRate(Integer channelType, String provider, double failRate) {
        failRateMap.put(channelType + ":" + provider, failRate);
        log.warn("【故障注入】{}:{} 失败率设为 {}", channelType, provider, failRate);
    }

    public double getFailRate(Integer channelType, String provider) {
        return failRateMap.getOrDefault(channelType + ":" + provider, 0.0);
    }

    public void clear() {
        failRateMap.clear();
        log.warn("【故障注入】已清空所有注入");
    }
}