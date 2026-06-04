# Day 6:RocketMQ 接入,发送变异步(2026-06-04)

## 今日目标
- [x] docker 起 RocketMQ namesrv + broker
- [x] MessageProducer / MessageConsumer / MessageTask
- [x] ExternalSendController 改成投 MQ 立即返回
- [x] 端到端跑通:发请求 → 秒回 messageIds → 消费端打日志

## 实际操作
1. docker-compose 增加 rmqnamesrv + rmqbroker 两个 service(镜像 apache/rocketmq:5.3.2),
   都挂到已有的 pulse-net 网络,加 JAVA_OPT_EXT 限制堆内存防笔记本 OOM。
2. 新建 docker/rocketmq/broker.conf:brokerIP1=127.0.0.1 + autoCreateTopicEnable=true。
3. pulse-app/pom.xml 加 rocketmq-spring-boot-starter 2.3.4。
4. application.yml 顶级节点加 rocketmq 配置(name-server / producer.group)。
5. 新建 mq 包三件套:MessageTask(MQ 任务体)/ MessageProducer(syncSend)/
   MessageConsumer(@RocketMQMessageListener 占位,只打日志)。
6. ExternalSendController 改造:删掉 Day5「打日志=假发送」,改成
   查模板 → 渲染(一次,全接收者共用)→ 循环 receivers 各生成 messageId → 投 MQ → 立即返回。

## 踩坑记录
| # | 坑 | 解决 |
|---|---|---|
| 1 | docker-compose 粘贴后 rmqnamesrv 顶格未缩进 → yaml line 66 报错 did not find expected key | 统一缩进 2 空格,子项对齐;两个新 service 补加 networks: pulse-net |
| 2 | 启动报 DefaultMQPushConsumer.setNamespaceV2 方法不存在 | starter 2.3.4 默认带的 rocketmq-client 5.1.4 太旧;exclude 掉后单独引 rocketmq-client 5.3.2(和服务端版本对齐) |

## 验证结果
- 接口返回:status=accepted, count=2, messageIds=[MSG_56121590936637440, MSG_56121592337534976]
- 模板渲染正确:"尊敬的张三,您的验证码是8888,5分钟内有效。"(name/code 替换成功)
- 控制台日志:生产者 2 条「消息入队成功 SEND_OK」+ 消费者 2 条「【MQ消费占位】收到发送任务」
- Redis 配额:pulse:quota:2:20260604 请求前后 +1

## 设计决策补充
- Topic / Group 约定:
    - topic = pulse_message_send
    - producer group = pulse_producer_group
    - consumer group = pulse_message_consumer_group
- 一个接收者 = 一条 MQ 消息(一个 MessageTask):每人后续要独立频控/路由/轨迹/重试,
  不把 List 塞一条消息里。
- 模板渲染留在 controller(同步),不下放消费端。原因:
  ① variables 全接收者共用,渲染一次即可,避免消费端重复渲染 N 次;
  ② 模板不存在/缺变量属于业务方传参错误,应同步快速失败,不该先回"已受理"再静默失败;
  ③ 真正要异步化的是"调渠道发送"(几百 ms~几秒),不是渲染(微秒级)。
- MessageTask 携带"已渲染好的 content",不带 variables。
- messageId(MSG_ + 雪花)在入队时生成,作为后续幂等/轨迹/死信主键。
- MessageProducer 用 syncSend,KEYS 设为 messageId,方便后续按 messageId 检索。

## 遗留 TODO(有意的技术债,非 bug)
- 配额仍是"每请求扣 1",与接收者数量无关。发给 2 人应扣 2,但批量扣减
  (Lua 一次扣 N + 扣到一半不足的回滚)归到 Day16 防超卖一起做,凑简历亮点2。
  controller 已留 TODO 注释标记。
- 沿用 Day5 旧账:GlobalExceptionHandler 把 NoResourceFoundException 当 500,理想应 404。

## 明日计划(Day 7)
- JMeter 压 /api/external/v1/send 接口,拿第一份 baseline(目标 1000+ QPS)
- 对比异步化前后吞吐 / 响应时间,数据写进简历金句
- Week1 总结 + 修一周内积累的小 bug
- 准备简历第一版关键词