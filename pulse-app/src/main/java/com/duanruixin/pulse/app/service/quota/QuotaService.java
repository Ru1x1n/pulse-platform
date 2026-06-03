package com.duanruixin.pulse.app.service.quota;

import com.duanruixin.pulse.app.entity.App;
import com.duanruixin.pulse.app.service.AppService;
import com.duanruixin.pulse.common.exception.BusinessException;
import com.duanruixin.pulse.common.result.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;

/**
 * 配额管理服务
 * Redis Key: pulse:quota:{appId}:{yyyyMMdd}
 * Value: 当日已使用配额
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuotaService {

    private static final String KEY_TEMPLATE = "pulse:quota:%d:%s";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * Lua 脚本:原子地查+判断+扣减
     * KEYS[1] = quota key
     * ARGV[1] = daily quota 上限
     * ARGV[2] = key 过期秒数(2 天)
     * 返回:1 成功,0 配额不足
     */
    private static final String DECR_QUOTA_LUA =
            "local used = tonumber(redis.call('GET', KEYS[1]) or '0') " +
                    "local limit = tonumber(ARGV[1]) " +
                    "if used >= limit then " +
                    "  return 0 " +
                    "end " +
                    "redis.call('INCR', KEYS[1]) " +
                    "redis.call('EXPIRE', KEYS[1], ARGV[2]) " +
                    "return 1";

    private final RedissonClient redissonClient;
    private final AppService appService;

    /**
     * 扣减配额,失败抛业务异常
     */
    public void deductQuota(Long appId) {
        App app = appService.getByIdCached(appId);
        if (app == null) {
            throw new BusinessException(ErrorCode.APP_NOT_FOUND);
        }

        String key = String.format(KEY_TEMPLATE, appId, LocalDate.now().format(DATE_FMT));

        RScript script = redissonClient.getScript(StringCodec.INSTANCE);
        Long result = script.eval(
                RScript.Mode.READ_WRITE,
                DECR_QUOTA_LUA,
                RScript.ReturnType.INTEGER,
                Collections.singletonList(key),
                String.valueOf(app.getDailyQuota()),
                "172800"   // 2 天过期(防止跨日数据残留)
        );

        if (result == null || result == 0) {
            log.warn("配额已用尽: appId={}, dailyQuota={}", appId, app.getDailyQuota());
            throw new BusinessException(ErrorCode.QUOTA_EXCEEDED);
        }
        log.debug("配额扣减成功: appId={}", appId);
    }

    /**
     * 查当日已用配额
     */
    public Long getUsedQuota(Long appId) {
        String key = String.format(KEY_TEMPLATE, appId, LocalDate.now().format(DATE_FMT));
        String val = (String) redissonClient.getBucket(key, StringCodec.INSTANCE).get();
        return val == null ? 0L : Long.parseLong(val);
    }
}