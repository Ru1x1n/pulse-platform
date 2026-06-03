package com.duanruixin.pulse.app.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ReUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.duanruixin.pulse.app.dto.TemplateCreateDTO;
import com.duanruixin.pulse.app.entity.Template;
import com.duanruixin.pulse.app.mapper.TemplateMapper;
import com.duanruixin.pulse.app.service.AppService;
import com.duanruixin.pulse.app.service.TemplateService;
import com.duanruixin.pulse.common.exception.BusinessException;
import com.duanruixin.pulse.common.result.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateServiceImpl extends ServiceImpl<TemplateMapper, Template> implements TemplateService {

    private final AppService appService;
    private final RedissonClient redissonClient;

    private static final String CACHE_KEY = "pulse:template:";
    private static final long CACHE_TTL_SECONDS = 600;

    /** 匹配 {{xxx}} 的变量正则 */
    private static final Pattern VAR_PATTERN = Pattern.compile("\\{\\{(\\w+)}}");

    @Override
    public Template createTemplate(TemplateCreateDTO dto) {
        // 1. 校验应用存在
        if (appService.getById(dto.getAppId()) == null) {
            throw new BusinessException(ErrorCode.APP_NOT_FOUND);
        }

        // 2. 校验 templateCode 唯一
        Long count = this.lambdaQuery()
                .eq(Template::getAppId, dto.getAppId())
                .eq(Template::getTemplateCode, dto.getTemplateCode())
                .count();
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "模板编码已存在");
        }

        // 3. 从 content 里提取变量列表(如 {{name}}, {{code}})
        Set<String> vars = new HashSet<>();
        ReUtil.findAll(VAR_PATTERN, dto.getContent(), 1, vars);

        Template template = new Template();
        template.setTemplateCode(dto.getTemplateCode());
        template.setAppId(dto.getAppId());
        template.setTemplateName(dto.getTemplateName());
        template.setChannelType(dto.getChannelType());
        template.setContent(dto.getContent());
        template.setVariables(JSON.toJSONString(new ArrayList<>(vars)));
        template.setStatus(1);

        this.save(template);
        log.info("模板创建成功: id={}, code={}", template.getId(), template.getTemplateCode());
        return template;
    }

    @Override
    public Template getByCodeCached(Long appId, String templateCode) {
        String cacheKey = CACHE_KEY + appId + ":" + templateCode;
        RBucket<Template> bucket = redissonClient.getBucket(cacheKey);

        Template cached = bucket.get();
        if (cached != null) {
            return cached;
        }

        Template template = this.lambdaQuery()
                .eq(Template::getAppId, appId)
                .eq(Template::getTemplateCode, templateCode)
                .eq(Template::getStatus, 1)
                .one();

        if (template != null) {
            bucket.set(template, Duration.ofSeconds(CACHE_TTL_SECONDS));
        }
        return template;
    }
}