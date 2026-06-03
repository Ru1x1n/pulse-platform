package com.duanruixin.pulse.app.controller;

import com.duanruixin.pulse.app.dto.TemplateCreateDTO;
import com.duanruixin.pulse.app.entity.Template;
import com.duanruixin.pulse.app.service.TemplateService;
import com.duanruixin.pulse.common.result.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/template")
@RequiredArgsConstructor
public class TemplateController {

    private final TemplateService templateService;

    @PostMapping
    public Result<Template> createTemplate(@Valid @RequestBody TemplateCreateDTO dto) {
        Template t = templateService.createTemplate(dto);
        return Result.success(t);
    }

    @GetMapping("/{id}")
    public Result<Template> getTemplate(@PathVariable("id") Long id) {
        return Result.success(templateService.getById(id));
    }

    @GetMapping("/by-app/{appId}")
    public Result<List<Template>> listByApp(@PathVariable("appId") Long appId) {
        List<Template> list = templateService.lambdaQuery()
                .eq(Template::getAppId, appId)
                .eq(Template::getStatus, 1)
                .list();
        return Result.success(list);
    }
}