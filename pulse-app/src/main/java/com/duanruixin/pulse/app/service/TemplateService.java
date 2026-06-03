package com.duanruixin.pulse.app.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.duanruixin.pulse.app.dto.TemplateCreateDTO;
import com.duanruixin.pulse.app.entity.Template;

public interface TemplateService extends IService<Template> {

    Template createTemplate(TemplateCreateDTO dto);

    /**
     * 根据 appId + templateCode 查模板(走缓存)
     */
    Template getByCodeCached(Long appId, String templateCode);
}