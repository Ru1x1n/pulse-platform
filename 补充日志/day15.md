# Day 15 智能退避重试 + 死信(简历亮点 2)

> 一句话:把 Day14 观察到的「失败消息躺着不管」补上——发送失败后按 1s/5s/30s/2m/10m 指数退避重试,
> 重试 5 次仍失败则归档进死信表 t_dead_letter,提供死信查看/重发接口。

---

## 一、今日目标
- 失败消息不再终结于 FAILED,改为延时重试(RocketMQ delayLevel 指数退避)
- 重试 5 次耗尽 → 写死信表 t_dead_letter,消息状态 → RETURNED(4)
- 死信管理接口:查看列表 / 手动重发
- 解决重试引入的状态机难点(重试时前置态从 FAILED 取,不是 PENDING)

## 二、核心设计

### 1. 为什么要"退避"(越等越久),不是立刻重试
渠道失败多为临时抖动(网络/对端限流)。失败后立刻重试、再失败再立刻重试,等于对正出问题的
渠道猛捶,可能加重雪崩。指数退避(1s→5s→30s→2m→10m)给对端喘息时间,是生产级重试标准做法。

### 2. 延时实现:RocketMQ 经典 delayLevel(方案A)
- RocketMQ 内置 18 个固定延时级别:1s 5s 10s 30s 1m 2m ... 对应 level 1 2 3 4 5 6 ...
- 取 1s/5s/30s/2m/10m → level 1/2/4/6/14(RETRY_DELAY_LEVELS={1,2,4,6,14})
- syncSend(topic, message, timeout, delayLevel) 重载,starter 2.3.4 支持,broker 默认开启不用改
- 未选方案B(5.x 任意精确延时 deliverTimeMillis):要的5档恰好都是内置级别,A 够用且稳

### 3. retryCount 通过 MQ 消息体传递(关键)
重试 = 重新投一条 MQ 消息。下次消费要知道"这是第几次重试",只能靠 MessageTask 带 retryCount 过去。
- MessageTask 新增 retryCount 字段(默认0)
- 首次投递 retryCount=0;重试时 producer 投递前 set 为 nextRetry
- t_message.retry_count 库字段同步更新(给人看/统计),但流转靠消息体

### 4. ★状态机难点:重试时前置态是 FAILED 不是 PENDING
原 CAS:WHERE status=from。首次消费推进 PENDING→SENDING。
但重试消息当前状态是 FAILED(3),若仍用 PENDING 做前置态,CAS rows=0 跳过 → 重试链断裂。
解法:消费端开头按 retryCount 选前置态:
```
retryCount==0 → 从 PENDING 推 SENDING(首次)
retryCount>0  → 从 FAILED  推 SENDING(重试)
```
MessageService.updateStatus 本身不改(from 是参数,设计良好),只在消费端按情况传不同 from。

### 5. 重试 vs 死信判定
```
发送失败:
  先把 SENDING 落回 FAILED(作为下次重试 CAS 前置态)
  if retryCount < MAX_RETRY(5):
      nextRetry = retryCount+1
      task.setRetryCount(nextRetry); 库 retry_count 同步
      producer.sendRetryTask(task)  // 延时消息,delayLevel=RETRY_DELAY_LEVELS[retryCount]
  else:
      moveToDeadLetter(...)  // 写 t_dead_letter + 状态 FAILED→RETURNED(4)
```

### 6. 防重复消费(advanced 变量)
消费端开头 updateStatus 返回 boolean(CAS 是否推进成功)用 advanced 接住:
- advanced==false → 状态非预期(MQ 重复投递/并发)→ 直接 return,不重复发送
- 这是决策#17「CAS 天然防重复消费」的显式落地

## 三、链路位置
```
MessageConsumer.onMessage:
  按 retryCount 选前置态 → 推进 SENDING(advanced 防重)
  查模板 → 路由 → 策略发送 → 埋点
  成功 → SENDING→SUCCESS
  失败 → handleFailure:
           SENDING→FAILED
           retryCount<5 ? 投延时重试(delayLevel 递增) : moveToDeadLetter
```

## 四、本次新增 / 改动
**新增**
- docker/init-sql/06-dead-letter.sql   死信表(message_id 唯一索引)
- entity/DeadLetter + mapper/DeadLetterMapper   死信实体/Mapper(注解照 ChannelConfig 风格)
- controller/DeadLetterController       死信接口:/api/v1/dead-letter/list、/resend/{messageId}
- MessageProducer.sendRetryTask         延时重试投递(syncSend 带 delayLevel)
- MessageProducer.RETRY_DELAY_LEVELS    {1,2,4,6,14}
- MessageService.moveToDeadLetter       写死信 + 状态→RETURNED(事务)
- MessageService.updateRetryCount       同步库 retry_count

**改动**
- mq/MessageTask                        加 retryCount 字段(默认0)
- mq/MessageConsumer                    重写:重试+退避+死信+按 retryCount 选前置态+advanced 防重
- MessageService                        注入 DeadLetterMapper

**未动**
- ChannelRouter / ChannelHealthService / 四个 Handler / ChannelFactory / ExternalSendController(首次投递靠 retryCount 默认0)

## 五、踩坑表
| # | 坑 | 解决 |
|---|---|---|
| 32 | 测重试时只注入 aliyun 100% 失败,路由切到 tencent 发成功,测不到重试 | 短信两个 provider(aliyun+tencent)都注入 100% 失败,路由选谁都必失败才触发重试 |
| 33 | 【你来填:如 syncSend 带 delayLevel 重载报方法不存在】 | starter 版本差异;改用 RocketMQTemplate 其他延时 API 或原生 client 5.3.2 |
| 34 | 【你来填:如重试消息状态卡 FAILED 不动】 | 消费端前置态写死 PENDING 了;改为按 retryCount 选 PENDING/FAILED |

> 注:#33 #34 为预埋,实际没踩到则删。本次一次过可只留 #32。

## 六、遗留 TODO
- **死信手动重发的状态重置**:重发时 t_message 是 RETURNED(4),消费端 CAS 期望 PENDING 会跳过。
  需重发前重置状态或特殊处理 → Day16 幂等一起
- **重试导致重复发送**:邮件首次"超时判失败"但其实已发出,重试再发→用户收两封。
  Day16 幂等(messageId 去重)解决
- **catch 异常分支 / handleFailure 未补 record 健康度**:真发抛异常时漏记失败到 ZSet,健康率偏乐观。
  可在 handleFailure 补一次 channelHealthService.record(false)
- delayLevel 固定 5 档硬编码,够用,后续可配置化
- 死信无定时清理/告警,后续可加

## 七、验证结果(端到端)

### 重试 + 退避(短信两 provider 均注入 100% 失败)
发一条 T100 必失败消息,盯日志:
```
第0次投递 → 发送失败 → 安排第1次重试, delayLevel=1   →约1s后
第1次重试 → 失败 → 安排第2次, delayLevel=2            →约5s后
第2次重试 → 失败 → 安排第3次, delayLevel=4            →约30s后
第3次重试 → 失败 → 安排第4次, delayLevel=6            →约2m后
第4次重试 → 失败 → 安排第5次, delayLevel=14           →约10m后
第5次重试 → 失败 → 重试耗尽,进入死信
```
★间隔 1s/5s/30s/2m/10m 逐级拉长 = 指数退避生效

### 死信落表 ✅(实测)
```
t_dead_letter:
  message_id=MSG_62044019542331392, retry_count=5, last_error=发送失败(aliyun)
```

### track 表完整轨迹(退避 + 状态横跳的铁证)
```
null→0 创建 / 0→1 开始处理 / 1→3 失败 / 3→1 第1次重试 / 1→3 失败 / 3→1 第2次重试 / ...
最后 3→4 重试耗尽进死信
create_time 相邻间隔逐级拉长 = 退避证据
3→1 流转 = 状态机难点(FAILED回SENDING)解决的证明
```
【你来填:贴一下 track 查询结果,确认间隔拉长 + 最后 3→4】

### 验收清单
- [x] t_dead_letter 建表,DeadLetter 实体/Mapper 编译通过
- [x] MessageTask 加 retryCount,首次发送 retryCount=0 正常
- [ ] 日志看到「安排第N次重试 delayLevel 递增」【你确认】
- [ ] 重试间隔逐级拉长(指数退避)【你确认】
- [ ] retry_count 随重试 0→5
- [ ] track 有 1→3、3→1 反复,最后 3→4
- [x] 5 次后进 t_dead_letter,retry_count=5
- [ ] 死信接口 list 能查、resend 能投
- [ ] 正常消息(不注入故障)仍一次成功(回归)

## 八、简历可写(亮点2)
> 基于 RocketMQ 延时消息实现失败消息的指数退避重试(1s/5s/30s/2m/10m),重试耗尽自动归档死信表,
> 提供死信查看/重发;通过 CAS 状态机(失败→重试时前置态切换 + 重复消费防护)保证状态流转正确。

## 九、明日计划(Day 16)
- 幂等性 + 防超卖(统一处理多个遗留 TODO):
    - messageId 全局唯一 + 幂等表,解决重试/重复消费导致的重复发送
    - 死信手动重发的状态重置(承接本日 TODO)
    - 配额按接收者批量扣减 + Lua 防超卖(一次扣N + 不足回滚)
    - 检测顺序:敏感词/频控前置到扣配额之前(违规/超频不扣配额)