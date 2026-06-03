package com.duanruixin.pulse.app.service;

import com.duanruixin.pulse.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TemplateRendererTest {

    private final TemplateRenderer renderer = new TemplateRenderer();

    @Test
    void should_render_template_correctly() {
        Map<String, String> vars = new HashMap<>();
        vars.put("name", "张三");
        vars.put("code", "8888");

        String result = renderer.render("尊敬的{{name}},验证码是{{code}}", vars);
        assertEquals("尊敬的张三,验证码是8888", result);
    }

    @Test
    void should_throw_when_variable_missing() {
        Map<String, String> vars = new HashMap<>();
        vars.put("name", "张三");
        // 故意不传 code

        assertThrows(BusinessException.class, () ->
                renderer.render("尊敬的{{name}},验证码{{code}}", vars));
    }

    @Test
    void should_handle_no_variables() {
        String result = renderer.render("纯文本无变量", new HashMap<>());
        assertEquals("纯文本无变量", result);
    }
}