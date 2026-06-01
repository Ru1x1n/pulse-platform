package com.duanruixin.pulse.app.controller;

import com.duanruixin.pulse.common.exception.BusinessException;
import com.duanruixin.pulse.common.result.ErrorCode;
import com.duanruixin.pulse.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/hello")
public class HelloController {

    @Value("${server.port}")
    private Integer port;

    @GetMapping
    public Result<Map<String, Object>> hello(
            @RequestParam(name = "name", required = false) String name) {  // ← 加 name = "name"
        log.info("hello called, name={}", name);
        Map<String, Object> data = new HashMap<>();
        data.put("message", "Hello " + (name == null ? "Pulse" : name));
        data.put("port", port);
        data.put("service", "pulse-app");
        return Result.success(data);
    }
    @GetMapping("/error")
    public Result<Void> testError() {
        // 故意抛业务异常,验证全局异常处理器是否生效
        throw new BusinessException(ErrorCode.APP_NOT_FOUND);
    }


}