package com.duanruixin.pulse.app.service.channel;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.duanruixin.pulse.app.entity.ChannelConfig;
import com.duanruixin.pulse.app.mapper.ChannelConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 渠道配置查询服务
 * TODO 性能优化(后续):候选渠道可加 Redis/本地缓存,当前查库走 idx_app_channel 索引
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChannelConfigService {

    private final ChannelConfigMapper channelConfigMapper;

    /**
     * 查某 app 某渠道类型下所有【启用】的服务商,按 priority 倒序(数字大=优先)
     */
    public List<ChannelConfig> listEnabled(Long appId, Integer channelType) {
        return channelConfigMapper.selectList(
                Wrappers.<ChannelConfig>lambdaQuery()
                        .eq(ChannelConfig::getAppId, appId)
                        .eq(ChannelConfig::getChannelType, channelType)
                        .eq(ChannelConfig::getStatus, 1)
                        .orderByDesc(ChannelConfig::getPriority)
        );
    }
}