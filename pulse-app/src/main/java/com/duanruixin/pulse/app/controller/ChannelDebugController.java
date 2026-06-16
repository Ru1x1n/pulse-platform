package com.duanruixin.pulse.app.controller;

import com.duanruixin.pulse.app.service.channel.ChannelFaultInjector;
import com.duanruixin.pulse.app.service.channel.ChannelHealth;
import com.duanruixin.pulse.app.service.channel.ChannelHealthService;
import com.duanruixin.pulse.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 渠道调试接口(临时,仅测试用,Day13 删除)
 * 走 /api/v1/** 后台路径,无鉴权
 */
@RestController
@RequestMapping("/api/v1/debug/channel")
@RequiredArgsConstructor
public class ChannelDebugController {

    private final ChannelFaultInjector faultInjector;
    private final ChannelHealthService channelHealthService;

    /** 注入故障:给某渠道服务商设失败率 */
    @PostMapping("/fault")
    public Result<String> injectFault(@RequestParam Integer channelType,
                                      @RequestParam String provider,
                                      @RequestParam double failRate) {
        faultInjector.setFailRate(channelType, provider, failRate);
        return Result.success(provider + " 失败率已设为 " + failRate);
    }

    /** 清空所有故障注入 */
    @DeleteMapping("/fault")
    public Result<String> clearFault() {
        faultInjector.clear();
        return Result.success("已清空故障注入");
    }

    /** 查看某渠道服务商当前健康度 */
    @GetMapping("/health")
    public Result<Map<String, Object>> health(@RequestParam Integer channelType,
                                              @RequestParam String provider) {
        ChannelHealth h = channelHealthService.getHealth(channelType, provider);
        return Result.success(Map.of(
                "provider", provider,
                "total", h.total(),
                "fail", h.fail(),
                "failRate", h.failRate()
        ));
    }
}