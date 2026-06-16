package com.duanruixin.pulse.app.service.channel;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * 渠道实时健康度(5分钟滑动窗口成功率)
 * total key: pulse:channel:total:{type}:{provider}  全量发送流水
 * fail  key: pulse:channel:fail:{type}:{provider}   仅失败流水(member 与 total 相同,是其子集)
 * 数据结构: ZSet,member = now+":"+UUID(防同毫秒覆盖),score = 毫秒时间戳
 * 失败率 = ZCARD(fail)/ZCARD(total),两个 ZCARD 都是 O(1)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChannelHealthService {

    private static final String TOTAL_KEY = "pulse:channel:total:%d:%s";
    private static final String FAIL_KEY  = "pulse:channel:fail:%d:%s";

    /** 滑动窗口 5 分钟 */
    private static final long WINDOW_MS = 5 * 60 * 1000L;
    /** key TTL 比窗口略长 */
    private static final long TTL_SECONDS = 600L;

    /**
     * 记录一次发送结果(原子)
     * KEYS[1]=total key  KEYS[2]=fail key
     * ARGV[1]=now  ARGV[2]=windowStart  ARGV[3]=member  ARGV[4]=isFail('1'/'0')  ARGV[5]=ttl
     */
    private static final String RECORD_LUA =
            "local now = tonumber(ARGV[1]) " +
                    "local windowStart = tonumber(ARGV[2]) " +
                    "redis.call('ZADD', KEYS[1], now, ARGV[3]) " +
                    "redis.call('ZREMRANGEBYSCORE', KEYS[1], 0, windowStart) " +
                    "redis.call('EXPIRE', KEYS[1], tonumber(ARGV[5])) " +
                    "if ARGV[4] == '1' then " +
                    "  redis.call('ZADD', KEYS[2], now, ARGV[3]) " +
                    "  redis.call('ZREMRANGEBYSCORE', KEYS[2], 0, windowStart) " +
                    "  redis.call('EXPIRE', KEYS[2], tonumber(ARGV[5])) " +
                    "end " +
                    "return 0";

    /**
     * 查询窗口内 {total, fail}(查询前先清过期,避免久未发送的脏数据)
     * KEYS[1]=total key  KEYS[2]=fail key
     * ARGV[1]=windowStart
     * 返回: {total, fail}
     */
    private static final String QUERY_LUA =
            "redis.call('ZREMRANGEBYSCORE', KEYS[1], 0, tonumber(ARGV[1])) " +
                    "redis.call('ZREMRANGEBYSCORE', KEYS[2], 0, tonumber(ARGV[1])) " +
                    "local total = redis.call('ZCARD', KEYS[1]) " +
                    "local fail = redis.call('ZCARD', KEYS[2]) " +
                    "return {total, fail}";

    private final RedissonClient redissonClient;

    /** 记录一次发送结果 */
    public void record(Integer channelType, String provider, boolean success) {
        String totalKey = String.format(TOTAL_KEY, channelType, provider);
        String failKey  = String.format(FAIL_KEY, channelType, provider);
        long now = System.currentTimeMillis();
        String member = now + ":" + UUID.randomUUID();

        RScript script = redissonClient.getScript(StringCodec.INSTANCE);
        script.eval(
                RScript.Mode.READ_WRITE,
                RECORD_LUA,
                RScript.ReturnType.INTEGER,
                Arrays.asList(totalKey, failKey),
                String.valueOf(now),
                String.valueOf(now - WINDOW_MS),
                member,
                success ? "0" : "1",
                String.valueOf(TTL_SECONDS)
        );
    }

    /** 查询窗口内健康度 */
    public ChannelHealth getHealth(Integer channelType, String provider) {
        String totalKey = String.format(TOTAL_KEY, channelType, provider);
        String failKey  = String.format(FAIL_KEY, channelType, provider);
        long windowStart = System.currentTimeMillis() - WINDOW_MS;

        RScript script = redissonClient.getScript(StringCodec.INSTANCE);
        List<Long> result = script.eval(
                RScript.Mode.READ_WRITE,
                QUERY_LUA,
                RScript.ReturnType.MULTI,
                Arrays.asList(totalKey, failKey),
                String.valueOf(windowStart)
        );
        long total = (result != null && result.size() > 0) ? result.get(0) : 0L;
        long fail  = (result != null && result.size() > 1) ? result.get(1) : 0L;
        return new ChannelHealth(total, fail);
    }
}