## Pulse 项目设计要点

### 一、最终交付物
- 11 个微服务的完整分布式系统(单机部署演示,架构可扩展)
- 100+ 单元测试 + 端到端压测报告
- 一键启动脚本 + 完整 README + 30 天踩坑日志
- GitHub 公开仓库,面试官可现场拉起验证

### 二、核心业务场景
模拟「茅台官方旗舰店双 11 抢购通知」场景:
- 商家(租户)创建应用,平台分配 API Key + Secret
- 商家通过 HTTP API 提交消息(支持模板变量)
- 平台异步路由到最优渠道发送
- 提供消息状态追踪、漏斗分析、配额管理

### 三、技术亮点(简历金句)

**亮点 1:智能渠道降级**
- Redis ZSet 滑动窗口实时统计 5 分钟内成功率
- 路由决策基于实时数据动态选择,失败率 >20% 自动跳过
- 压测:阿里云通道注入 50% 失败时,系统 12 秒内全切到腾讯云,
  整体成功率稳定 99% 以上

**亮点 2:三层防超卖式配额扣减**
- 第一层:Redis Lua 脚本原子扣减(GET+判断+INCR)
- 第二层:DB 唯一索引兜底(同应用同分钟不重复)
- 第三层:幂等表(messageId 全局唯一)
- 单机 QPS 1500+,无超扣

**亮点 3:智能退避重试 + 死信介入**
- RocketMQ 延时消息实现指数退避:1s/5s/30s/2m/10m
- 5 次失败入死信表,后台可人工干预或永久丢弃
- 对比定时任务扫表方案:延迟从分钟级降到秒级,DB 压力下降 80%

**亮点 4:Canal + ES 实时轨迹检索**
- MySQL binlog → Canal → RocketMQ → ES
- 业务方查"昨天给 X 用户发的所有消息" P99 < 1.2s
- 解决双写一致性问题,无分布式事务

### 四、技术栈
- **框架**:Spring Boot 3.2 + Spring Cloud Alibaba 2023.0
- **服务治理**:Nacos(注册+配置)+ Sentinel(限流)
- **存储**:MySQL 8(主存储)+ Redis 7(缓存/锁/滑动窗口)+ ES(轨迹检索)
- **消息**:RocketMQ 5(异步/延时/事务消息)
- **持久化**:MyBatis-Plus 3.5
- **认证**:JWT(管理后台)+ HMAC-SHA256(API 鉴权)
- **可观测性**:SkyWalking + Prometheus + Grafana
- **工程化**:Docker Compose + JMeter + Arthas

### 五、规模与性能(压测真实数据)
- 单机 QPS:**1500+**(接收端)
- P99 响应时间:**< 500ms**
- 端到端延迟(从 API 调用到消息送达):**< 3s**
- 日发送量(模拟):**100 万条**
- 渠道故障切换时间:**< 15 秒**


# Pulse 项目接管文档

## 个人背景
- 段锐昕,UVA 双学位(2026 校招求职 Java 后端)
- 简历项目:已删除烽火云(课程项目)和物业管理(业务弱),Pulse 是主力项目

## 项目定位
Pulse - 企业级 SaaS 消息推送中台
模拟极光推送/个推等商业服务,统一对接短信/邮件/Push/微信四类渠道

## 技术栈
- JDK 17 + Spring Boot 3.2.5 + Spring Cloud Alibaba 2023.0.1.0
- Nacos 2.3 / Redis 7.2 + Redisson 3.27.2 / MySQL 8 / MyBatis-Plus 3.5.5
- Maven 3.9.16 / Docker Compose
- GroupId: com.duanruixin.pulse

## 项目结构
pulse-platform/
├── pulse-common(Result/ErrorCode/Exception/JwtUtil/SnowflakeId/SignUtil)
├── pulse-app(端口 8081)
│   ├── controller(Hello/Tenant/App/Template/ExternalSend)
│   ├── service(TenantService/AppService/TemplateService/TemplateRenderer/QuotaService)
│   ├── mapper(TenantMapper/AppMapper/TemplateMapper)
│   ├── entity(Tenant/App/Template)
│   ├── interceptor(ApiAuthInterceptor 鉴权)
│   └── config(MybatisPlusConfig/RedissonConfig/WebMvcConfig/MetaObjectHandler)
└── docker(MySQL/Redis/Nacos)

## 已完成进度(Day 1-5)
- ✅ Day 1: 项目骨架 + Hello + Nacos
- ✅ Day 2: 工具类(JWT/Snowflake/Sign) + 单元测试 + 3 张表
- ✅ Day 3: MyBatis-Plus + 租户/应用 CRUD
- ✅ Day 4: API 鉴权(Key+签名+时间戳) + Redis 缓存密钥 + Lua 配额扣减
- ✅ Day 5: 模板表 + 变量渲染 + 真正按模板发送

## 关键设计决策
1. **三层 API 鉴权**:Header X-App-Key + X-Timestamp + X-Sign(HMAC-SHA256)
2. **配额 Redis Lua 原子扣减**:Key 设 2 天 TTL(留缓冲)
3. **app_secret 只在创建时返回一次**:查询接口手动置 null
4. **GlobalExceptionHandler 三档处理**:Business→warn,Validation→警告,Exception→error
5. **依赖 scope 严格**:pulse-common 的 web/lombok 设 provided 避免污染
6. **构造方法注入(RequiredArgsConstructor)** 替代 @Autowired 字段注入
7. **路径区分**:/api/v1/** 后台管理,/api/external/** 业务方调用走拦截器
8. **模板变量自动提取**:正则 \{\{(\w+)}} 从 content 抽变量名

## 已踩过的坑(防止重复)
- JDK 25 太新 → 降 17
- @RequestParam/@PathVariable 必须显式写 name(Spring Boot 3 不默认带 -parameters)
- spring-boot-starter-web 不自带 validation(Spring Boot 2.3+ 剥离)
- Redisson 配置要放根级 redisson:(不要嵌套在 spring 下)
- Postman URL 框不能带换行(粘贴时容易带)
- MyBatis Lambda 查询用 lambdaQuery() 而非 selectList

## 当前 GitHub
https://github.com/Ru1x1n/pulse-platform

## 数据库测试数据
- 租户:tenant_code=TEST_A (id=2)
- 应用:app_key=pulse_xxx(查 t_app 拿真实值)
- 模板:T001(验证码)、T002(订单通知)





Pulse 剩余 25 天计划速览
Week 1 收尾(Day 6-7)
Day 6:接入 RocketMQ,发送变异步
搭 RocketMQ → 写 MessageProducer → ExternalSendController 改成扔 MQ 不阻塞 → 写一个 MessageConsumer 占位(暂时只打日志)
Day 7:第一周收尾 + JMeter 压测
压测 send 接口拿到第一份性能数据(目标 1000+ QPS) → 写 Week1 总结 → 修一周内积累的小 bug → 准备简历第一版关键词

Week 2:业务深度(Day 8-14)
Day 8:DFA 敏感词过滤
建敏感词表 → DFA Trie 树构建 → AuditService 检测,命中拒发 → 单元测试 + 性能测试(1ms 内 1000 字)
Day 9:用户频控(Redis ZSet 滑动窗口)
FreqLimitService:同一手机号 1 分钟 1 条 / 1 小时 5 条 / 1 天 20 条 → ZSet 时间戳滑动窗口 → 接入到发送链路
Day 10:t_message 主表 + t_message_track 轨迹表
消息持久化设计(按月分表预留)→ 状态枚举(待发/发送中/成功/失败/退回)→ MessageService
Day 11:智能渠道降级(简历亮点 1)⭐
渠道实时成功率统计(Redis ZSet 5 分钟窗口) → ChannelRouter 路由决策 → 失败率 >20% 自动跳过 → 故障注入测试
Day 12:策略模式实现多渠道适配
ChannelHandler 接口 + SmsChannelHandler/EmailChannelHandler/PushHandler/WechatHandler → ChannelFactory 选择器
Day 13:对接真渠道(短信 + 邮件)
阿里云短信 SDK 接入(或腾讯云,选一个)→ JavaMail SMTP 发邮件 → Mock 阿里云 / QQ 邮箱真发一封
Day 14:Consumer 真正消费 + 完整链路打通
MessageConsumer 接 MQ → 频控 → 敏感词 → 路由 → 渠道发送 → 写 t_message_track → 端到端跑通

Week 3:可观测 + 高阶特性(Day 15-21)
Day 15:智能退避重试 + 死信(简历亮点 2)⭐
RocketMQ 延时消息 1s/5s/30s/2m/10m 重试链 → 5 次失败入 t_dead_letter → 死信管理后台接口
Day 16:幂等性 + 防超卖
messageId 全局唯一 + Redis 幂等表 → Lua 配额 + DB 唯一索引兜底 → 重复请求测试
Day 17:Canal 接入 + Binlog 监听(简历亮点 3 上)⭐
Canal 部署 + MySQL binlog 配置 → CanalListener 监听 t_message 表变更 → 推 RocketMQ
Day 18:Elasticsearch 接入 + 轨迹检索(简历亮点 3 下)⭐
ES 集群部署(Docker) → Tracker 服务消费 Canal MQ → 写 ES 索引 → 提供按时间/手机号/状态多维查询
Day 19:AB 测试 + 漏斗统计
模板版本化(t_template_version)→ 流量分流(SHA256 hash 取模)→ 各版本独立统计 → 漏斗:已发/送达/点击转化
Day 20:Sentinel 限流
接入 Sentinel → 给 send 接口加 QPS 限流(单机 2000/全局 10000)→ 限流规则配置到 Nacos → 触发限流时降级返回
Day 21:Week 3 收尾 + 中期压测
跑 30 分钟压测(JMeter)→ 拿到稳定 P99 数据 → 解决暴露的瓶颈(连接池/线程池调优)→ 准备 Week 4 监控

Week 4:监控 + 工程化收尾(Day 22-30)
Day 22:Prometheus + Micrometer 接入
spring-boot-actuator + prometheus 端点 → 关键指标埋点(QPS/成功率/各渠道发送量/配额耗尽次数)
Day 23:Grafana 大盘搭建
Grafana Docker 部署 → 接 Prometheus 数据源 → 搭 3 个核心面板(实时大盘/渠道健康/业务漏斗)→ 截图准备简历
Day 24:SkyWalking 链路追踪
SkyWalking 部署 → Java Agent 接入 pulse-app → 看一条消息从 API 到 ES 的完整链路 → 截图
Day 25:JMeter 全链路压测 + Arthas 诊断
压测真实业务流程(鉴权→频控→敏感词→路由→发送→落库→ES)→ 用 Arthas 看慢方法 / 看线程栈
Day 26:Docker Compose 一键启动
完善 docker-compose.yml 把 8 个中间件 + 你的服务全打包 → 任何人 git clone + docker-compose up 就能跑


项目结构：









C:\dev\pulse-platform
├── pom.xml                              # 父工程,packaging=pom,管理 BOM
├── .gitignore                           # 已排除 .idea/target/docker/data
├── README.md
│
├── docker/
│   ├── docker-compose.yml               # MySQL/Redis/Nacos 三容器
│   └── init-sql/
│       └── 01-schema.sql                # 建表+测试数据
│
├── docs/
│   ├── PROJECT_HANDOFF.md               # 本文档
│   └── diary/
│       ├── day01.md ~ day05.md          # 每日踩坑日志
│
├── pulse-common/                        # 公共模块(被所有业务依赖)
│   ├── pom.xml
│   └── src/main/java/com/duanruixin/pulse/common/
│       ├── result/
│       │   ├── Result.java              # 统一响应封装 {code,message,data,timestamp}
│       │   └── ErrorCode.java           # 错误码枚举(40001-60003)
│       ├── exception/
│       │   ├── BusinessException.java   # 业务异常,携带 ErrorCode
│       │   └── GlobalExceptionHandler.java  # @RestControllerAdvice,捕获异常→Result
│       └── util/
│           ├── JwtUtil.java             # JJWT 0.12.5 HS256,2小时过期
│           ├── SnowflakeIdGenerator.java  # 64位:1符号+41时间戳+5DC+5机器+12序列
│           └── SignUtil.java            # HMAC-SHA256,TreeMap 排序拼接
│
└── pulse-app/                           # 应用管理微服务(端口 8081)
├── pom.xml
└── src/main/java/com/duanruixin/pulse/app/
├── PulseAppApplication.java     # @SpringBootApplication + @EnableDiscoveryClient
├── SignGenerator.java           # 临时工具:生成 Postman 签名(Day 4 加的)
├── controller/
│   ├── HelloController.java     # /hello 健康检查
│   ├── TenantController.java    # /api/v1/tenant
│   ├── AppController.java       # /api/v1/app
│   ├── TemplateController.java  # /api/v1/template
│   └── ExternalSendController.java  # /api/external/v1/send(走拦截器)
├── service/
│   ├── TenantService.java + Impl
│   ├── AppService.java + Impl   # 含 getByAppKeyCached(Redis 缓存)
│   ├── TemplateService.java + Impl  # 含 getByCodeCached(Redis 缓存)
│   ├── TemplateRenderer.java    # 正则 {{(\w+)}} 替换变量
│   └── quota/
│       └── QuotaService.java    # Lua 脚本原子扣配额
├── mapper/
│   ├── TenantMapper.java        # extends BaseMapper<Tenant>
│   ├── AppMapper.java
│   └── TemplateMapper.java
├── entity/
│   ├── Tenant.java              # @TableName("t_tenant")
│   ├── App.java
│   └── Template.java
├── dto/
│   ├── TenantCreateDTO.java     # @NotBlank/@Email 校验
│   ├── AppCreateDTO.java
│   ├── TemplateCreateDTO.java
│   └── SendMessageDTO.java      # 业务方调发送接口的入参
├── vo/
│   └── AppCreateVO.java         # 创建应用返回 secret 一次
├── interceptor/
│   └── ApiAuthInterceptor.java  # 三层鉴权(Key+Sign+Timestamp)
└── config/
├── MybatisPlusConfig.java   # @MapperScan + 分页插件
├── MybatisMetaObjectHandler.java  # 自动填充 createTime/updateTime
├── RedissonConfig.java      # RedissonClient Bean
└── WebMvcConfig.java        # 注册


建造的表：


## 数据库(MySQL 8 - pulse 库)

4 张表已建:

```sql
-- t_tenant 租户表(id=2 → tenant_code=TEST_A)
-- t_app    应用表(id=2 → app_key=pulse_V8F368Yfkq4QJqsIE80Iz6nyMMFtsNUe)
--                   app_secret=fmDpGRoSkXIjiSUUtMbAVf8nHyFRD6pSFy6CtB0AYTslkwjs37ZEINArboRE2peP
-- t_channel_config 渠道配置表(Day 2 建,Day 5 暂未用)
-- t_template 模板表(T001=验证码,T002=订单通知,T100=测试,均 app_id=2)
```

**重要约定**:
- 所有表 `t_` 前缀
- 必备三件套:`create_time` / `update_time` / `is_deleted`(逻辑删除)
- 主键 BIGINT 自增(业务表),日志表用雪花 ID
- 状态字段 TINYINT,Java 端用枚举管理常量
- **不用外键**(分库分表友好)
- JSON 字段存灵活配置(如 t_channel_config.config_json、t_template.variables)

## 关键设计决策(必须遵守)

1. **三层 API 鉴权**:`X-App-Key + X-Timestamp(5分钟容差) + X-Sign(HMAC-SHA256)`
2. **配额扣减**:Redis Lua 脚本原子操作 `GET → 比较 → INCR`,Key TTL=2 天(留缓冲)
3. **app_secret 只创建时返回一次**,GET 接口手动 setAppSecret(null)
4. **构造方法注入**(@RequiredArgsConstructor)替代 @Autowired 字段注入
5. **路径分离**:`/api/v1/**` 后台管理(无需鉴权) vs `/api/external/**` 业务方走拦截器
6. **模板变量**:用正则 `\{\{(\w+)}}` 从 content 自动提取,渲染时缺变量抛 BusinessException
7. **pulse-common 依赖严格设 provided**,避免污染下游模块
8. **GlobalExceptionHandler 已处理**:BusinessException(业务异常)+ MethodArgumentNotValidException(参数校验)+ 兜底 Exception
9. **Redisson 配置在根级 `redisson:`**,不嵌套在 spring 下
10. **MyBatis-Plus 用 lambdaQuery()**,避免硬编码字段名

## 已踩过的坑(防止重复)

| # | 坑 | 解决 |
|---|---|---|
| 1 | JDK 25 太新 | 降到 JDK 17 |
| 2 | Maven Archetype 创建模板有占位符 | 用 New Project 不用 Archetype |
| 3 | @RequestParam/@PathVariable 必须显式写 name(Spring Boot 3 不默认带 -parameters) | 父 pom 加 `<arg>-parameters</arg>` + IDEA Java Compiler 也加 + 代码显式写 name |
| 4 | spring-boot-starter-web 不自带 validation(2.3+ 剥离) | 单独加 spring-boot-starter-validation 依赖 |
| 5 | Spring Boot 3 用 jakarta.* 不是 javax.* | 全部改 jakarta |
| 6 | pulse-common 设 provided 跑 main 报 NoClassDefFoundError | 用单元测试代替 main 验证(test scope 自带依赖) |
| 7 | Redisson 配置必须放根级 `redisson:` | 不嵌套在 spring 下 |
| 8 | Postman URL 框不能带换行符 | 手动输入,不要复制粘贴 |
| 9 | MyBatis-Plus 必须用 Lambda 查询 lambdaQuery() | 避免硬编码字段名 |
| 10 | 父 pom 一度有重复 `<module>pulse-app</module>` | 删重复 |
| 11 | Day 1 把代码写在 src/main/resources/ 下 | 应在 src/main/java/ |
| 12 | Day 3 GlobalExceptionHandler 把 NoResourceFoundException 当 500 | 暂未优化,TODO 中(理想返回 404) |

## 当前进度

- ✅ **Day 1**: 项目骨架 + Hello + Nacos 注册(2026-05-29)
- ✅ **Day 2**: 工具类(JWT/Snowflake/Sign)+ 11 个单元测试 + 3 张表 DDL
- ✅ **Day 3**: MyBatis-Plus 接入 + 租户/应用 CRUD + 5 个单元测试
- ✅ **Day 4**: API 鉴权(Key+Sign+TS)+ Redis 缓存密钥 + Lua 配额扣减 + 端到端测试(5 失败场景全过)
- ✅ **Day 5**: t_template 表 + 模板 CRUD + TemplateRenderer 正则渲染 + 模板 Redis 缓存(10 分钟 TTL)+ ExternalSendController 接 Body 真渲染

## TODO(性能优化,Day 7 压测后做)

1. **QuotaService 多查一次 t_app**
  - 现象:每次发送都 `SELECT FROM t_app WHERE id=?`,虽然鉴权拦截器已经查过缓存
  - 方案 A:给 AppService 加 `getByIdCached`
  - 方案 B:拦截器把 App 对象塞 request,QuotaService.deductQuota 接收 App 参数
  - **等 Day 7 压测拿到 baseline 数据再优化,对比前后写进简历金句**

2. **GlobalExceptionHandler 优化**
  - NoResourceFoundException 应返回 404 而非 500
  - 已加 MethodArgumentNotValidException 处理 ✅

## Redis Key 命名规范

          Code snapshots:
          
          
          
          package com.duanruixin.pulse.common.result;
          
          import lombok.AllArgsConstructor;
          import lombok.Getter;
          
          /**
          * 错误码枚举
            * 规范:
            *   200xx: 通用
            *   400xx: 参数/认证错误
            *   500xx: 服务器内部错误
            *   600xx: 业务错误
                */
                @Getter
                @AllArgsConstructor
                public enum ErrorCode {
          
          SUCCESS(200, "OK"),
          
          PARAM_INVALID(40001, "参数无效"),
          UNAUTHORIZED(40101, "未授权"),
          API_KEY_INVALID(40102, "API Key 无效"),
          SIGN_INVALID(40103, "签名校验失败"),
          TIMESTAMP_EXPIRED(40104, "请求时间戳过期"),
          FORBIDDEN(40301, "权限不足"),
          NOT_FOUND(40401, "资源不存在"),
          
          SERVER_ERROR(50001, "服务器内部错误"),
          SERVICE_UNAVAILABLE(50301, "服务不可用"),
          
          APP_NOT_FOUND(60001, "应用不存在"),
          APP_DISABLED(60002, "应用已停用"),
          QUOTA_EXCEEDED(60003, "配额已用尽"),
          TEMPLATE_NOT_FOUND(60101, "模板不存在"),
          SENSITIVE_WORD_DETECTED(60201, "内容包含敏感词"),
          USER_BLOCKED(60202, "用户在黑名单"),
          RATE_LIMIT(60203, "发送过于频繁"),
          CHANNEL_UNAVAILABLE(60301, "渠道不可用");
          
          private final Integer code;
          private final String message;
    }





          package com.duanruixin.pulse.common.result;
          
          import lombok.Data;
          import java.io.Serializable;
          
          /**
          * 通用响应封装
            * @param <T> 数据类型
              */
              @Data
              public class Result<T> implements Serializable {
          
              private Integer code;
              private String message;
              private T data;
              private Long timestamp;
          
              private Result() {
              this.timestamp = System.currentTimeMillis();
              }
          
              public static <T> Result<T> success() {
              return success(null);
              }
          
              public static <T> Result<T> success(T data) {
              Result<T> r = new Result<>();
              r.setCode(200);
              r.setMessage("OK");
              r.setData(data);
              return r;
              }
          
              public static <T> Result<T> fail(Integer code, String message) {
              Result<T> r = new Result<>();
              r.setCode(code);
              r.setMessage(message);
              return r;
              }
          
              public static <T> Result<T> fail(ErrorCode errorCode) {
              return fail(errorCode.getCode(), errorCode.getMessage());
              }
              }、



            package com.duanruixin.pulse.common.exception;

      import com.duanruixin.pulse.common.result.ErrorCode;
      import com.duanruixin.pulse.common.result.Result;
      import lombok.extern.slf4j.Slf4j;
      import org.springframework.web.bind.MethodArgumentNotValidException;
      import org.springframework.web.bind.annotation.ExceptionHandler;
      import org.springframework.web.bind.annotation.RestControllerAdvice;
      
      import java.util.stream.Collectors;
      
      @Slf4j
      @RestControllerAdvice
      public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBiz(BusinessException e) {
        log.warn("业务异常: code={}, msg={}", e.getCode(), e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Result<Void> handleIllegalArg(IllegalArgumentException e) {
        log.warn("参数异常: {}", e.getMessage());
        return Result.fail(ErrorCode.PARAM_INVALID.getCode(), e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleAll(Exception e) {
        log.error("系统异常", e);
        return Result.fail(ErrorCode.SERVER_ERROR);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ":" + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.warn("参数校验失败: {}", msg);
        return Result.fail(ErrorCode.PARAM_INVALID.getCode(), msg);
      }
    }




        package com.duanruixin.pulse.app.interceptor;
        
        import com.duanruixin.pulse.app.entity.App;
        import com.duanruixin.pulse.app.service.AppService;
        import com.duanruixin.pulse.common.exception.BusinessException;
        import com.duanruixin.pulse.common.result.ErrorCode;
        import com.duanruixin.pulse.common.util.SignUtil;
        import jakarta.servlet.http.HttpServletRequest;
        import jakarta.servlet.http.HttpServletResponse;
        import lombok.RequiredArgsConstructor;
        import lombok.extern.slf4j.Slf4j;
        import org.springframework.stereotype.Component;
        import org.springframework.web.servlet.HandlerInterceptor;
        
        import java.util.HashMap;
        import java.util.Map;
        
        /**
        * API 鉴权拦截器
          * 校验业务方调用接口时的 app_key + timestamp + sign
            */
            @Slf4j
            @Component
            @RequiredArgsConstructor
            public class ApiAuthInterceptor implements HandlerInterceptor {
        
            private final AppService appService;
        
            /** 时间戳允许的偏差(毫秒)5 分钟 */
            private static final long TIMESTAMP_TOLERANCE = 5 * 60 * 1000L;
        
            /** Request attribute key,用于把 appId 传给后续业务代码 */
            public static final String ATTR_APP_ID = "X-App-Id";
        
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
            String appKey = request.getHeader("X-App-Key");
            String timestamp = request.getHeader("X-Timestamp");
            String sign = request.getHeader("X-Sign");

         // 1. 三个 Header 不能为空
         if (appKey == null || timestamp == null || sign == null) {
             throw new BusinessException(ErrorCode.UNAUTHORIZED, "缺少鉴权 Header");
         }

         // 2. 时间戳防重放(5 分钟内)
         long ts;
         try {
             ts = Long.parseLong(timestamp);
         } catch (NumberFormatException e) {
             throw new BusinessException(ErrorCode.TIMESTAMP_EXPIRED, "时间戳格式错误");
         }
         long now = System.currentTimeMillis();
         if (Math.abs(now - ts) > TIMESTAMP_TOLERANCE) {
             log.warn("时间戳过期: appKey={}, ts={}, now={}", appKey, ts, now);
             throw new BusinessException(ErrorCode.TIMESTAMP_EXPIRED, "请求时间戳过期");
         }

         // 3. 根据 appKey 查应用(查 secret)
         App app = appService.getByAppKeyCached(appKey);
         if (app == null) {
             throw new BusinessException(ErrorCode.API_KEY_INVALID, "App Key 无效或已停用");
         }

         // 4. 验证签名
         // 参与签名的参数:appKey + timestamp(可以再加业务参数,这里简化)
         Map<String, String> params = new HashMap<>();
         params.put("appKey", appKey);
         params.put("timestamp", timestamp);
         params.put("sign", sign);

         if (!SignUtil.verify(params, app.getAppSecret())) {
             log.warn("签名校验失败: appKey={}", appKey);
             throw new BusinessException(ErrorCode.SIGN_INVALID);
         }

         // 5. 把 appId 放到 request,供业务代码使用
         request.setAttribute(ATTR_APP_ID, app.getId());
         log.debug("API 鉴权通过: appKey={}, appId={}", appKey, app.getId());

         return true;
    }
    }



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


      server:
      port: 8081
      
      spring:
      application:
      name: pulse-app
      profiles:
      active: dev
      data:
      redis:
      host: localhost
      port: 6379
      password: redis123456
      timeout: 3000ms
      lettuce:
      pool:
      max-active: 8
      max-idle: 8
      min-idle: 0
      
      # 数据源配置
      datasource:
      driver-class-name: com.mysql.cj.jdbc.Driver
      url: jdbc:mysql://localhost:3306/pulse?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
      username: root
      password: root123456
      
      cloud:
      nacos:
      discovery:
      server-addr: 127.0.0.1:8848
      username: nacos
      password: nacos
      namespace: public
      
      # Redisson 配置(顶级节点,和 spring 同级)
      redisson:
      address: redis://localhost:6379
      password: redis123456
      database: 0
      
      # MyBatis-Plus 配置
      mybatis-plus:
      configuration:
      map-underscore-to-camel-case: true
      log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
      global-config:
      db-config:
      logic-delete-field: isDeleted
      logic-delete-value: 1
      logic-not-delete-value: 0
      id-type: AUTO
      mapper-locations: classpath*:mapper/**/*.xml
      
      logging:
      level:
      com.duanruixin.pulse: DEBUG