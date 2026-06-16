Day 11 智能渠道降级(简历亮点 1)


一句话:在同一渠道类型下的多个服务商(provider)之间,按「最近 5 分钟实时失败率」自动选最优;
某 provider 失败率 >20% 自动跳过切备用;全部超阈值时降级到「最不坏」的而非丢弃消息。




一、今日目标


渠道实时成功率统计:Redis ZSet 5 分钟滑动窗口(total / fail 两个 key)
ChannelRouter 路由决策:按 provider 优先级 + 实时失败率选渠道
失败率 >20% 自动跳过该 provider,切到备用 provider
故障注入测试:把 aliyun 打成高失败率,验证自动秒切 tencent
把 Day10 MessageConsumer 里「模拟成功」的 TODO,换成「真路由 + Mock 发送 + 记录健康度」


二、核心设计

1. 成功率存储:两个 ZSet,不用单 key 前缀标记

pulse:channel:total:{channelType}:{provider}   每次发送都记 1 条(全量)
pulse:channel:fail:{channelType}:{provider}    仅失败记 1 条(fail 是 total 的子集,member 相同)

失败率 = ZCARD(fail) / ZCARD(total)    ← 两个 ZCARD 都是 O(1)


member = now + ":" + UUID(防同毫秒覆盖,与 Day9 频控同源)
score = 毫秒时间戳;滑动窗口靠 ZREMRANGEBYSCORE 清「5 分钟前」实现
TTL 600s(比窗口略长);record / query 各一段 Lua,保证原子一致
为什么不用单 ZSet 存 S_/F_ 前缀:那样数失败要把窗口内 member 全拉出来循环判前缀,O(N);
在 2500+ QPS、每条消息都要路由的热路径上会拖垮 Redis。两个 key 各 ZCARD 是 O(1)。
代价仅是「失败时多一次 ZADD」(成功消息根本不碰 fail key),拿写换读,划算。


2. 路由决策(ChannelRouter)

route(appId, channelType):
候选 = ChannelConfigService.listEnabled(appId, channelType)   # status=1, 按 priority 倒序
候选为空 ───────────────────────────────► 抛 CHANNEL_UNAVAILABLE(60301)

按优先级从高到低遍历每个 provider:
health = getHealth(type, provider)
┌─ total < MIN_SAMPLES(10) ─► 样本不足,当健康 ─► 选它   ★关键①
├─ failRate <= 20%         ─► 健康          ─► 选它(高优先先得)
└─ failRate > 20%          ─► 跳过,但记下「最不坏候选」(failRate 最小者)
全部 > 20% ──► 返回最不坏的那个 + warn 日志              ★关键②(降级不丢消息)


★关键① 样本下限 MIN_SAMPLES=10:刚发 1 条就失败 = 1/1 = 100%,没有下限会被立刻误判跳过;
0 发送是 0/0 未定义。所以窗口内样本不足一律当健康。这是「失败率类判断」的通用坑,
也是熔断器 half-open 给机会的思路。
★关键② 全坏兜底:所有 provider 都 >20% 时不能把消息丢了(比降级更糟),
选当前最不坏的发出去 + warn。只有「一个渠道都没配」才真抛 60301。


3. Mock 渠道 + 故障注入(Day13 接真渠道后删)


MockChannelSender:按 ChannelFaultInjector 设定的失败率随机判定成功/失败
ChannelFaultInjector:内存 Map 存 {channelType:provider → 失败率},压测时手动注故障
ChannelDebugController:临时调试接口(/api/v1/debug/channel/**,无鉴权),注故障 / 查健康度


三、链路位置(本次只动消费端,主链路不碰)

ExternalSendController.send(不变)
扣配额→查模板→渲染→敏感词→[for 收件人]频控→落库→投MQ→立即返回
↓ MQ
MessageConsumer.onMessage(本次重写 Day10「模拟成功」)
PENDING→SENDING
查模板拿 channelType(MessageTask 已带 appId + templateCode,反查不改消息体)
ChannelRouter.route(appId, channelType)  ── 按失败率选 provider  ★Day11核心
MockChannelSender.send(...)              ── 故障注入,模拟成功/失败
ChannelHealthService.record(...)         ── 两 key 埋点(成败)
成功 SENDING→SUCCESS / 失败 SENDING→FAILED(重试/死信留 Day15)


说明:这不是擅自重构,是 Day10 本就留的 TODO 点(「这里换成真实渠道发送」)。
Day11 先用 Mock 填位(卖点是路由决策,Mock+故障注入比真渠道更能演示「失败率高自动切换」);
Day13 把 MockChannelSender 换成真 SDK 即可,onMessage 结构不再动。



四、本次新增 / 改动文件

新增


enums/ChannelType                          渠道类型枚举(1短信/2邮件/3推送/4微信,对齐 MessageStatus 风格)
entity/ChannelConfig + mapper/ChannelConfigMapper   渠道配置表实体(注解照抄 Template)
service/channel/ChannelConfigService       查候选渠道(lambdaQuery,status=1,priority 倒序)
service/channel/ChannelHealthService       两 key 成功率,record + getHealth,各一段 Lua
service/channel/ChannelHealth(record)      健康度快照,failRate()(total=0 返回 0)
service/channel/MockChannelSender          Mock 发送
service/channel/ChannelFaultInjector       故障注入器
service/channel/ChannelRouter              ★路由决策核心
controller/ChannelDebugController          临时调试接口(Day13 删)
docker/init-sql/04-channel-config.sql      测试数据:app_id=2 短信配 aliyun(优先20)+ tencent(备用10)


改动


mq/MessageConsumer                         整文件替换,Day10「模拟成功」→ 真路由 + Mock + 埋点 + try/catch 落 FAILED


五、踩坑表

#坑解决22docker cp 报 GetFileAttributesEx ...\docker\docker: cannot find the file已 cd 到 docker 目录,命令里又写了 docker/init-sql/,路径重复多了一层。去掉开头 docker/,或退回项目根目录再 cp23PowerShell curl -X POST 注故障行为古怪PowerShell 的 curl 是 Invoke-WebRequest 别名,带 query 的 POST 不稳。改用 Invoke-RestMethod -Method Post -Uri "..."24redis-cli 报 NOAUTH Authentication requiredRedis 设了密码,redis-cli 没带。加 -a redis123456 --no-auth-warning(--no-auth-warning 消掉密码明文警告)25【你来填:如改过 T100 channel_type 但没清模板缓存,路由拿旧 type 走「无渠道」分支,可记此坑】改 DB 后清模板缓存 / 重启 app(getByCodeCached 走缓存,改库不清缓存读旧值)

六、遗留 TODO


候选渠道查库未缓存:消费端每条消息查一次 t_channel_config(走 idx_app_channel 索引)。
想榨性能可加 Redis/本地缓存(与 baseline「想榨再做」思路一致,先留)
故障 Map(ChannelFaultInjector)是单机内存,多实例不共享(仅测试用,Day13 删,不影响)
ChannelDebugController + ChannelFaultInjector 是临时调试件,Day13 接真渠道时一并删除
失败率阈值 20% / 样本下限 10 / 窗口 5 分钟,目前硬编码,后续可配置化(同频控阈值 TODO)
失败后只是落 FAILED,没有重试/退避/死信 → Day15 处理(retry_count 也在 Day15 启用)


七、验证结果(端到端,不只看接口 200)

故障注入实测(把 aliyun 打成 80% 失败,连发不同手机号 ≥15 条)

关键日志(亮点1的「灵魂证据」):

【路由】跳过 provider=aliyun(失败率0.4>0.2), total=10, fail=4
【路由】选中 provider=tencent(样本不足8/10,视为健康)
【Mock发送】provider=tencent, messageId=MSG_604..., 失败率=0.0, 结果=成功
UPDATE t_message SET status=2 ... WHERE message_id=MSG_604... AND status=1   ← CAS 防重
INSERT INTO t_message_track ... from_status=1, to_status=2, remark=发送成功(tencent)

证明:


✅ 失败率突破 20% 阈值 → aliyun 被自动跳过
✅ ★关键① 样本下限生效(tencent 8/10 视为健康,不被小样本误判跳过)
✅ 降级不丢消息(切 tencent 真发出去)
✅ 状态机 CAS(WHERE status=1)+ 双表(t_message 覆盖 / t_message_track 追加)正确


Redis 两 key 副作用核对

ZCARD pulse:channel:total:1:aliyun   = 【你来填】
ZCARD pulse:channel:fail:1:aliyun    = 【你来填】   (fail/total ≈ 注入的失败率)
ZCARD pulse:channel:total:1:tencent  = 【你来填】
ZCARD pulse:channel:fail:1:tencent   = 【你来填】   (≈ 0,健康)
TTL   pulse:channel:total:1:aliyun   = 【你来填】   (≤ 600)

DB 状态核对

t_message:        有 status=2(成功) 也有 status=3(失败,aliyun 那几条)   【你来填条数】
t_message_track:  有 1→2 和 1→3 两种流水,remark 标着 aliyun/tencent

验收清单


不注入时走 aliyun(最高优先级)
aliyun 注入高失败率 + 样本攒够后,日志「跳过 aliyun → 选中 tencent」
状态机 CAS + 双表正确
Redis 两 key ZCARD 与发送量对得上(填完上面数字打勾)
TTL ≤ 600
t_message 有 status=2 和 3,track 有对应流水


八、简历可写(亮点1)


设计基于 Redis ZSet 5 分钟滑动窗口的渠道实时成功率统计(total/fail 双 key,O(1) 取数),
实现智能渠道降级路由:服务商失败率 >20% 自动跳过切换备用渠道,全部异常时降级到最优而非丢弃,
配合样本下限避免小样本误判。故障注入测试验证秒级切换。



九、明日计划(Day 12)


策略模式多渠道适配:ChannelHandler 接口 + Sms/Email/Push/Wechat Handler + ChannelFactory
为 Day13 接真渠道(邮件真发 JavaMail + 短信保持 Mock)铺好接口结构
把 MockChannelSender 的「发送」抽象成策略,Day13 替换实现而不动路由/消费端