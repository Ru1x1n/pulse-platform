package com.duanruixin.pulse.app.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.duanruixin.pulse.app.dto.AppCreateDTO;
import com.duanruixin.pulse.app.entity.App;
import com.duanruixin.pulse.app.entity.Tenant;
import com.duanruixin.pulse.app.mapper.AppMapper;
import com.duanruixin.pulse.app.service.AppService;
import com.duanruixin.pulse.app.service.TenantService;
import com.duanruixin.pulse.common.exception.BusinessException;
import com.duanruixin.pulse.common.result.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor   // Lombok,自动生成全参构造,实现构造方法注入
public class AppServiceImpl
        extends ServiceImpl<AppMapper, App>
        implements AppService {

    private final TenantService tenantService;
    private final RedissonClient redissonClient;

    /** 应用缓存 key 模板 */
    private static final String CACHE_KEY = "pulse:app:key:";
    /** 缓存过期时间:10 分钟 */
    private static final long CACHE_TTL_SECONDS = 600;
    @Override
    public App getByAppKeyCached(String appKey) {
        String cacheKey = CACHE_KEY + appKey;
        RBucket<App> bucket = redissonClient.getBucket(cacheKey);

        // 1. 查缓存
        App cached = bucket.get();
        if (cached != null) {
            log.debug("命中缓存: {}", cacheKey);
            return cached;
        }

        // 2. 缓存未命中,查 DB
        App app = this.lambdaQuery()
                .eq(App::getAppKey, appKey)
                .eq(App::getStatus, 1)
                .one();

        // 3. 回写缓存(即使是 null 也缓存,防穿透 —— 后面 Day 5 会优化)
        if (app != null) {
            bucket.set(app, java.time.Duration.ofSeconds(CACHE_TTL_SECONDS));
            log.debug("写入缓存: {}", cacheKey);
        }

        return app;
    }

    @Override
    public App createApp(AppCreateDTO dto) {
        // 1. 校验租户存在
        Tenant tenant = tenantService.getById(dto.getTenantId());
        if (tenant == null) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "租户不存在");
        }
        if (tenant.getStatus() == 0) {
            throw new BusinessException(ErrorCode.APP_DISABLED, "租户已停用");
        }

        // 2. 生成 app_key 和 app_secret
        String appKey = "pulse_" + RandomUtil.randomString(32);
        String appSecret = RandomUtil.randomString(64);

        // 3. 构建实体保存
        App app = new App();
        app.setTenantId(dto.getTenantId());
        app.setAppName(dto.getAppName());
        app.setAppKey(appKey);
        app.setAppSecret(appSecret);
        app.setDailyQuota(dto.getDailyQuota() == null ? 10000 : dto.getDailyQuota());
        app.setStatus(1);

        this.save(app);
        log.info("应用创建成功: id={}, name={}, key={}", app.getId(), app.getAppName(), app.getAppKey());
        return app;
    }

    @Override
    public List<App> listByTenantId(Long tenantId) {
        return this.lambdaQuery()
                .eq(App::getTenantId, tenantId)
                .eq(App::getStatus, 1)
                .orderByDesc(App::getCreateTime)
                .list();
    }
}