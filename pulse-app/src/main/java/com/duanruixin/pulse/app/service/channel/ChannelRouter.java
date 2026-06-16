package com.duanruixin.pulse.app.service.channel;

import com.duanruixin.pulse.app.entity.ChannelConfig;
import com.duanruixin.pulse.common.exception.BusinessException;
import com.duanruixin.pulse.common.result.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 渠道路由器(智能降级核心)
 * 在同一渠道类型下的多个服务商(provider)之间,按实时失败率选择最优,
 * 失败率 >20% 自动跳过;全部超阈值时降级到"最不坏"的而非丢弃。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChannelRouter {

    /** 失败率阈值:超过则跳过该 provider */
    private static final double FAIL_THRESHOLD = 0.20;
    /** 窗口内最小样本数,不足则当健康(避免小样本误判) */
    private static final long MIN_SAMPLES = 10;

    private final ChannelConfigService channelConfigService;
    private final ChannelHealthService channelHealthService;

    /**
     * 选择一个可用渠道服务商
     * @return 选中的 ChannelConfig(含 provider)
     */
    public ChannelConfig route(Long appId, Integer channelType) {
        List<ChannelConfig> candidates = channelConfigService.listEnabled(appId, channelType);
        if (candidates.isEmpty()) {
            log.warn("【路由】无可用渠道: appId={}, channelType={}", appId, channelType);
            throw new BusinessException(ErrorCode.CHANNEL_UNAVAILABLE,
                    "无可用渠道配置: channelType=" + channelType);
        }

        ChannelConfig fallback = null;
        double minFailRate = Double.MAX_VALUE;

        for (ChannelConfig c : candidates) {
            ChannelHealth health = channelHealthService.getHealth(channelType, c.getProvider());

            // ★关键①:样本不足当健康,直接选(高优先先得)
            if (health.total() < MIN_SAMPLES) {
                log.debug("【路由】选中 provider={}(样本不足{}/{},视为健康)",
                        c.getProvider(), health.total(), MIN_SAMPLES);
                return c;
            }

            double failRate = health.failRate();
            if (failRate <= FAIL_THRESHOLD) {
                log.debug("【路由】选中 provider={}(失败率{}≤{})",
                        c.getProvider(), failRate, FAIL_THRESHOLD);
                return c;
            }

            // 超阈值:跳过,但记下最不坏的
            log.warn("【路由】跳过 provider={}(失败率{}>{}), total={}, fail={}",
                    c.getProvider(), failRate, FAIL_THRESHOLD, health.total(), health.fail());
            if (failRate < minFailRate) {
                minFailRate = failRate;
                fallback = c;
            }
        }

        // ★关键②:全部超阈值,降级到最不坏的(不丢消息)
        log.warn("【路由】全部渠道失败率超阈值,降级到最不坏 provider={}, failRate={}",
                fallback.getProvider(), minFailRate);
        return fallback;
    }
}