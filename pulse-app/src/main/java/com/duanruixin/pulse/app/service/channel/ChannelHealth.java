package com.duanruixin.pulse.app.service.channel;

/**
 * 渠道健康度快照(窗口内)
 */
public record ChannelHealth(long total, long fail) {

    /** 失败率,total=0 时返回 0(无样本视为健康) */
    public double failRate() {
        return total == 0 ? 0.0 : (double) fail / total;
    }
}