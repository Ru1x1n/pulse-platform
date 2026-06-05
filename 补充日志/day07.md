# Day 7:JMeter 压测 send 接口,拿第一份 baseline(2026-06-05)

## 今日目标
- [x] 搭 JMeter 测试计划(动态签名)压 /api/external/v1/send
- [x] 拿到第一份可信 baseline(目标 QPS 1000+)
- [x] 验证数据真实性(配额计数 ≈ 请求数)

## Baseline 结果(真实有效)
环境:单机本地,JMeter 与 pulse-app 同机;50 线程 / Ramp-up 5s / 持续 60s
- QPS(Throughput):2562.93 /s(超目标 1500+ 约 70%)
- 总请求:151,587,Error 0.00%
- Average 18.68ms / Median 18ms / P95 27ms / P99 34ms / Max 79ms
- APDEX 1.000
- 交叉验证:Redis 配额计数 151588 ≈ #Samples 151587(差 1 为冒烟手动请求)→ 每条请求都真正走完全流程

## 关键过程 / 踩坑(本日最值钱的部分)
| # | 坑 | 现象 | 根因 | 解决 |
|---|---|---|---|---|
| 1 | 第一次压测假数据 | QPS 4359、P99 17ms、0 错误,但配额计数=0 | JMeter Header Manager 引用 ${appkey},脚本里存的是 appKey(大小写不符);JMeter 变量找不到时原样发字面量 → 服务端收到 X-App-Key=${appkey} → 鉴权 40102 全拒,根本没走业务逻辑 | Header 改 ${appKey},与 vars.put 大小写对齐 |
| 2 | 配额 key 查不到 | GET pulse:quota:2:20260605 返回 nil | 时区:LocalDate.now() 取 JVM 默认时区(本机美东),与容器 Asia/Shanghai 不一致,配额记到错误日期 | 见遗留 TODO,Day7 不修 |
| 3 | redis 命令行里敲 docker / 复制了 > 提示符 | unknown command | 操作失误 | 退出 redis 用 docker exec 单行执行 |

## 数据可信度方法论(写进简历能讲)
- 压测不能只看 200/QPS,必须交叉验证「业务副作用」:用 Redis 配额计数 ≈ 请求数,证明请求真的执行了逻辑,而非被拒后快速返回。
- 假 baseline(4359)vs 真 baseline(2563):前者是"被拒绝的速度",后者是"真发消息的速度"。
- 测量边界诚实标注:单机本地、压测机与应用同机抢 CPU,真实分布式部署会更高。

## 压测前置(已固化,后续 Day21/25 复用)
- application-perf.yml:NoLoggingImpl + WARN,避免 console I/O 成瓶颈;启动激活 dev,perf
- t_app.daily_quota 调至 1 亿,避免配额打爆污染错误率
- JSR223 PreProcessor(Groovy)动态算 HMAC-SHA256 签名,复刻 SignUtil
- 测量用非 GUI 模式(-n),GUI 仅搭计划/调试

## 遗留 TODO
1. 【真 bug,择期修】时区:QuotaService 用 LocalDate.now() 依赖 JVM 时区,配额按天边界错乱。
   修法:LocalDate.now(ZoneId.of("Asia/Shanghai")) 或 JVM 加 -Duser.timezone=Asia/Shanghai。
   素材价值:分布式系统所有时间须显式锁时区。
2. 【性能优化,有 baseline 可对比】QuotaService.deductQuota 里 getByIdCached 仍会查一次 t_app,
   而鉴权拦截器刚查过。优化(拦截器把 App 塞 request / 加 getByIdCached 真缓存)后重压对比,写金句。
3. 【Day16】配额"每请求扣 1" → 按接收者数批量扣减 + 防超卖。

## 产物留底
- report3 已另存为 report3-baseline-day7,作为后续优化前后对比基准(勿覆盖)

## 简历关键词(第一版)
- RocketMQ 异步解耦,接收端单机 QPS 2500+,P99 34ms,15万+请求零失败
- 压测数据可信度交叉验证(业务副作用核对)
- HMAC-SHA256 API 鉴权 + 动态签名压测方案

## 明日计划(Day 8)
- DFA 敏感词过滤:敏感词表 → Trie 树构建 → AuditService 命中拒发 → 单测 + 性能测试(1ms 内 1000 字)