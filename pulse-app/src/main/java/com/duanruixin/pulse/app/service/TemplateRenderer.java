package com.duanruixin.pulse.app.service;

import com.duanruixin.pulse.common.exception.BusinessException;
import com.duanruixin.pulse.common.result.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 模板渲染引擎
 * 把 {{name}} 这类占位符替换成实际值
 */
@Slf4j
@Component
public class TemplateRenderer {

    private static final Pattern VAR_PATTERN = Pattern.compile("\\{\\{(\\w+)}}");

    /**
     * 渲染模板
     *
     * @param template 模板内容,含 {{var}} 占位符
     * @param variables 变量 Map
     * @return 渲染后的字符串
     */
    public String render(String template, Map<String, String> variables) {
        if (template == null || template.isEmpty()) {
            return "";
        }

        Matcher matcher = VAR_PATTERN.matcher(template);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String varName = matcher.group(1);
            String value = variables == null ? null : variables.get(varName);
            if (value == null) {
                throw new BusinessException(ErrorCode.PARAM_INVALID,
                        "缺少变量: " + varName);
            }
            matcher.appendReplacement(result, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(result);

        return result.toString();
    }
}