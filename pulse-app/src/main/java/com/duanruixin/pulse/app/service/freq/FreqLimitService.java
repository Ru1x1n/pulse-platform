package com.duanruixin.pulse.app.service.freq;

import com.duanruixin.pulse.common.exception.BusinessException;
import com.duanruixin.pulse.common.result.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 用户频控服务(同 app + 手机号维度)
 * Redis Key: pulse:freq:{appId}:{receiver}
 * 数据结构: ZSet,member = 唯一值,score = 发送毫秒时间戳
 * 一个 ZSet 同时支撑 1分钟/1小时/1天 三个滑动窗口
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FreqLimitService {

    private static final String KEY_TEMPLATE = "pulse:freq:%d:%s";

    /** 限流阈值(Day9 硬编码,TODO 后续可配置化) */
    private static final int LIMIT_MINUTE = 1;   // 1 分钟最多 1 条
    private static final int LIMIT_HOUR   = 5;   // 1 小时最多 5 条
    private static final int LIMIT_DAY    = 20;  // 1 天最多 20 条

    /**
     * Lua:原子地 清理过期 + 三窗口计数判断 + 记录
     * KEYS[1] = freq key
     * ARGV[1] = now(ms)
     * ARGV[2..4] = 分/时/天 阈值
     * ARGV[5] = 本次唯一 member
     * 返回:0 通过(已记录),1 超分钟限,2 超小时限,3 超天限
     */
    private static final String FREQ_LUA =
            "local now = tonumber(ARGV[1]) " +
                    "local minAgo = now - 60000 " +
                    "local hourAgo = now - 3600000 " +
                    "local dayAgo = now - 86400000 " +
                    "redis.call('ZREMRANGEBYSCORE', KEYS[1], 0, dayAgo) " +
                    "if redis.call('ZCOUNT', KEYS[1], minAgo, now) >= tonumber(ARGV[2]) then return 1 end " +
                    "if redis.call('ZCOUNT', KEYS[1], hourAgo, now) >= tonumber(ARGV[3]) then return 2 end " +
                    "if redis.call('ZCOUNT', KEYS[1], dayAgo, now) >= tonumber(ARGV[4]) then return 3 end " +
                    "redis.call('ZADD', KEYS[1], now, ARGV[5]) " +
                    "redis.call('EXPIRE', KEYS[1], 86400) " +
                    "return 0";

    private final RedissonClient redissonClient;

    /**
     * 检查并记录一次发送,超频抛 RATE_LIMIT
     */
    public void checkAndRecord(Long appId, String receiver) {
        String key = String.format(KEY_TEMPLATE, appId, receiver);
        long now = System.currentTimeMillis();
        // member 必须唯一,否则同毫秒的两条会因 member 相同被 ZADD 覆盖
        String member = now + ":" + UUID.randomUUID();

        RScript script = redissonClient.getScript(StringCodec.INSTANCE);
        Long result = script.eval(
                RScript.Mode.READ_WRITE,
                FREQ_LUA,
                RScript.ReturnType.INTEGER,
                Collections.singletonList(key),
                String.valueOf(now),
                String.valueOf(LIMIT_MINUTE),
                String.valueOf(LIMIT_HOUR),
                String.valueOf(LIMIT_DAY),
                member
        );

        if (result == null || result == 0) {
            log.debug("频控通过: appId={}, receiver={}", appId, receiver);
            return;
        }

        String reason = switch (result.intValue()) {
            case 1 -> "每分钟最多 " + LIMIT_MINUTE + " 条";
            case 2 -> "每小时最多 " + LIMIT_HOUR + " 条";
            case 3 -> "每天最多 " + LIMIT_DAY + " 条";
            default -> "发送过于频繁";
        };
        log.warn("频控拦截: appId={}, receiver={}, 原因={}", appId, receiver, reason);
        throw new BusinessException(ErrorCode.RATE_LIMIT, "发送过于频繁:" + reason);
    }
}