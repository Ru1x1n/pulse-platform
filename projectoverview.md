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
