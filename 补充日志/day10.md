# Day 10:消息持久化 t_message + t_message_track(2026-06-10)

## 今日目标
- [x] t_message 主表 + t_message_track 轨迹表(按月分表预留)
- [x] MessageStatus 状态枚举(待发/发送中/成功/失败/退回)
- [x] MessageService:saveMessage 落库 + updateStatus CAS 推进 + 写轨迹
- [x] 接入:controller 投 MQ 前落库待发;消费者推进状态(模拟成功)
- [x] 端到端:主表 status=2成功,轨迹 3 条流转

## 两张表的分工(核心)
- t_message 主表 = "当前快照":一条消息一行,status 被反复 update 覆盖,回答"现在什么状态"
- t_message_track 轨迹表 = "历史流水":一条消息多行,只追加不修改,回答"经历了哪些变化、几点变的"
- 类比快递:主表="已签收";轨迹表="揽收→运输→派送→签收"的完整物流轨迹
- 为什么拆两张:主表 status 是最新值会覆盖,存不下历史;轨迹表把每次状态变化追加记录,可还原消息一生
- 二者靠 message_id 关联(全局唯一,系统的"消息身份证")

## 核心设计
- 主键:t_message 自增 BIGINT + message_id 唯一索引;关联全靠 message_id 不靠自增 id
- 状态机:待发0→发送中1→成功2/失败3;失败可重试回发送中或转退回4(Day14/15)
- 落库时机:controller 投 MQ 前落库(消息一进来即可查),消费端推进状态
- updateStatus 用 CAS 条件更新(WHERE message_id=? AND status=from):
  天然防重复消费(第二次 rows=0 跳过),是 Day16 幂等的雏形,先白拿一层保护
- @Transactional:改主表 + 写轨迹原子(saveMessage / updateStatus 都加)
- DB 存 TINYINT,Java 枚举 MessageStatus 管常量(不引 MyBatis 枚举 handler,不动全局配置)

## 按月分表"预留"(非实现)
- 无外键(分表友好)/ message_id 含雪花时间戳(可做路由键)/ create_time 加索引
- 不引 ShardingSphere,避免过度设计;简历讲"设计预留了分表路径"

## 踩坑记录
| # | 坑 | 现象 | 根因 | 解决 |
|---|---|---|---|---|
| 1 | 改完消费者状态不推进 | 主表 status 停在 0待发,轨迹只有 1 条"消息创建" | 改了 MessageConsumer(加注入+updateStatus)后没重启 pulse-app,跑的还是 Day6 旧版"只打日志"消费者 | 重启 app;新消息状态机正常跑通 3 条流转 |

## 关键经验
- 改了任何 Spring Bean / 编译型代码,必须重启 app 才生效。否则一直在测旧版,
  现象诡异(代码明明改了却没效果)。排查口诀:先确认"改完重启了吗"。
- 旧版发的消息(MSG_...49024)永远停在待发=脏数据,逻辑删即可,不影响功能。

## DB-MQ 时序确认
- controller send 方法无 @Transactional → saveMessage 独立事务调用完即提交,
  消费者总能查到"待发"记录,CAS 匹配成功,无赛跑。轨迹 3 条齐全已验证。
- (若 controller 包大事务,saveMessage 不单独提交,消费可能快于提交 → CAS rows=0 推不动。
  本项目当前无此问题,记此分支待 Day15/16 事务消息/本地消息表统一解决。)

## 验证结果
- t_message: MSG_...56928 status=2(成功)
- t_message_track: 3 条 —— NULL→0 消息创建 / 0→1 开始处理 / 1→2 模拟发送成功

## 遗留 TODO
1. 【Day14】消费者"模拟发送成功"换成真实渠道发送(频控/敏感词也搬到消费端)
2. 【Day15/16】失败→重试→退回 状态流转;retry_count 启用;
   "落库成功但投MQ失败"靠本地消息表/事务消息
3. 【Day18】基于 message_id 的多维查询(时间/手机号/状态)走 ES,不压 MySQL

## 明日计划(Day 11)⭐ 简历亮点1
- 智能渠道降级:渠道实时成功率 ZSet 5分钟滑动窗口 → ChannelRouter 路由决策
  → 失败率>20% 自动跳过 → 故障注入测试
- ZSet 套路与 Day9 频控同源,可复用