# Day 9:用户频控(Redis ZSet 滑动窗口)(2026-06-10)

## 今日目标
- [x] FreqLimitService:ZSet 滑动窗口,同 app+手机号 1分钟1条/1小时5条/1天20条
- [x] Lua 原子:清理过期 + 三窗口计数判断 + 记录
- [x] 集成测试通过(连发第二条被限 / 不同手机号互不影响)
- [ ] 接入发送链路(receiver 级,循环内,投 MQ 前)— 待确认
- [ ] 端到端:连发第二条被限 / 换号通过 — 待确认

## 核心设计
- 一个 ZSet 支撑三个窗口:member=唯一值,score=毫秒时间戳
- 滑动窗口:ZCOUNT key (now-窗口) now 数窗口内记录数,超阈值拒
- ZREMRANGEBYSCORE 清理超过最大窗口(1天)的旧记录,ZSet 不无限涨
- 检查+记录用 Lua 保证原子(同配额思路,防并发超发)
- member = now + UUID 保唯一,避免同毫秒两条被 ZADD 覆盖(漏计)
- 用 currentTimeMillis 毫秒戳,不碰 LocalDate → 无 Day7 时区坑
- key 维度 pulse:freq:{appId}:{receiver},多租户隔离(不同 app 互不影响)
- 阈值硬编码常量,Java 端 switch 映射拒绝原因

## 链路位置
扣配额 → 模板 → 渲染 → 敏感词(content 级,循环外)
→ for receiver { 频控 checkAndRecord(循环内) → 生成 messageId → 投 MQ }

## 与 Day8 敏感词的对比(便于复习)
- 敏感词:针对整条 content,一份,放循环外
- 频控:针对每个手机号,放 receivers 循环内
- 二者都是"命中即抛异常快速失败",不投 MQ

## 踩坑记录
| # | 坑 | 现象 | 根因 | 解决 |
|---|---|---|---|---|
| 1 | @SpringBootTest 测试类 @Autowired 标红 | "必须在有效 Spring Bean 中定义自动装配成员",但能 Run 且通过 | IDEA Spring 插件未把多模块子模块的 @SpringBootTest 测试类关联到 Spring 上下文,静态分析假阳性,非编译错误 | 可加 @SpringBootTest(classes=PulseAppApplication.class) 消红;不改也不影响运行 |
| 2 | 频控测试受历史数据干扰风险 | 老手机号 ZSet 已有记录,测"第一条通过"会失败 | 测试号之前发过,窗口里有残留 | 测试用随机手机号 "138"+时间戳;端到端前先 DEL freq key |

## 集成测试
- 同手机号一分钟内第二条被限 ✅(抛 BusinessException,msg 含"每分钟")
- 不同手机号互不影响 ✅
- 判断口诀记牢:能 Run 起来 = 不是编译错误,IDEA 红线只是静态分析看法

## 端到端验证(待填实际结果)
- 第1条 receivers=[13800000001] → __________
- 同号第2条(立即) → 期望 60203 每分钟最多1条,且无"消息入队成功" → __________
- 换号 receivers=[13900000002] → 期望 accepted → __________
- ZRANGE pulse:freq:2:13800000001 0 -1 WITHSCORES → 看到 member(ts:uuid)+score(ts)

## 遗留 TODO
1. 【Day16】批量 receiver 部分成功:前几个频控通过已投 MQ,后面超频拒整单 → 不一致。
   当前简单语义"超频拒整单",待统一定批量语义/回滚。
2. 【Day16】检测顺序:频控/敏感词应前置到扣配额之前(违规/超频不该扣配额)。
3. 【可配置化】限流阈值现硬编码,后续从 t_app 或 Nacos 读,支持每 app 不同档位。

## 明日计划(Day 10)
- t_message 主表 + t_message_track 轨迹表
- 状态枚举(待发/发送中/成功/失败/退回),Java 端枚举管理
- MessageService,消息持久化(按月分表预留)