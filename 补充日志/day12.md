# Day 12 策略模式多渠道适配

> 一句话:把 Day11「一个 MockChannelSender 糊弄所有渠道」的发送,拆成「一个渠道一个 Handler」的策略模式;
> 加新渠道 = 加一个 @Component 实现类,不改任何老代码(开闭原则)。为 Day13 接真渠道(邮件 JavaMail)铺好接口。

---

## 一、今日目标
- 定义 ChannelHandler 策略接口(support 声明负责的渠道 + send 发送)
- 四个渠道各一个 Handler:Sms / Email / Push / Wechat
- 故障注入逻辑收口到抽象基类 AbstractMockChannelHandler,子类只填渠道专属动作
- ChannelFactory 工厂:Spring 自动注入所有 Handler,按 channelType 建索引分发
- 消费端把 mockChannelSender.send(...) 换成 channelFactory.get(type).send(ctx)
- 删除 MockChannelSender(职责被 Handler 体系取代)

## 二、核心设计:策略模式 + 模板方法 + 工厂

### 1. 为什么要做(消灭未来的 if-else)
不做的话,真渠道接进来后 MockChannelSender 会变成:
```
if channelType==1 { 阿里云短信 SDK... }
else if ==2 { JavaMail SMTP... }
else if ==3 { APNs/FCM... }
else if ==4 { 微信公众号 API... }   ← 加渠道就加 else if,改老类有风险
```
一个类塞四种不相干逻辑、改一个碰坏另一个、加渠道必须改老代码。
策略模式拆开:每个渠道一个独立 Handler,加渠道 = 加新类,工厂和其他 Handler 一字不改。

### 2. 三个角色
```
ChannelHandler(接口)           岗位说明书:support()=我负责哪个渠道, send()=怎么发
  ├ SmsChannelHandler          短信专员(support→SMS)
  ├ EmailChannelHandler        邮件专员(support→EMAIL,Day13 换 JavaMail 真发)
  ├ PushChannelHandler         推送专员(support→PUSH)
  └ WechatChannelHandler       微信专员(support→WECHAT)

AbstractMockChannelHandler(基类)  共用培训手册:故障注入判定成败 + 打总日志(模板方法)
                                  子类只重写 doMockSend() 填渠道专属动作

ChannelFactory(工厂)            前台:启动时 Spring 注入 List<ChannelHandler> 自动收集全部,
                                  按 support() 建 EnumMap 索引;get(type) O(1) 分发
```

### 3. 策略模式最漂亮的一步:工厂自动认识所有 Handler
```java
private final List<ChannelHandler> handlers;   // Spring 自动注入接口的全部实现类
@PostConstruct init(): 遍历 handlers,按 support() 建 channelType→handler 的 Map
```
- 加新渠道(如钉钉)只需写 @Component 新 Handler,Factory 不改 → 开闭原则
- 同一渠道注册了两个 Handler → 启动期抛 IllegalStateException 暴露,不留到线上
- EnumMap 比 HashMap 更省更快(枚举专用)

### 4. 模板方法配合(基类管流程,子类管差异)
- send()(基类,固定流程):读故障注入失败率 → 随机判定成败 → 调 doMockSend → 打总日志 → 返回 boolean
- doMockSend()(子类,可变部分):各渠道专属动作。Mock 阶段只打渠道特有日志;
  Day13 邮件 Handler 重写它为真 SMTP 发送,其他渠道仍走 Mock,互不影响

## 三、链路位置(只重构「发送」这一步内部,路由/健康度/故障注入器全不动)
```
MessageConsumer.onMessage:
  PENDING→SENDING
  查模板拿 channelType
  ChannelRouter.route(...)            ── 选 provider(Day11,不动)
  ctx = new ChannelSendContext(channel, task)
  channelFactory.get(channelType).send(ctx)   ── 策略分发到对应 Handler(Day12 新)
  ChannelHealthService.record(...)   ── 埋点(Day11,不动)
  成功 SENDING→SUCCESS / 失败 SENDING→FAILED
```

## 四、本次新增 / 改动 / 删除
**新增(service/channel/handler 包)**
- ChannelSendContext           发送上下文(包 channelConfig + task,以后加字段不动 Handler 签名)
- ChannelHandler               策略接口(support + send)
- AbstractMockChannelHandler   抽象基类(故障注入 + 模板方法,子类填 doMockSend)
- SmsChannelHandler            短信(Day13 换阿里云/腾讯,Demo 保持 Mock)
- EmailChannelHandler          邮件(Day13 换 JavaMail SMTP 真发——Demo 主推真渠道)
- PushChannelHandler           推送(Mock)
- WechatChannelHandler         微信(Mock)
- ChannelFactory               策略工厂(自动注册 + 按 type 分发)

**改动**
- mq/MessageConsumer           mockChannelSender.send(...) → channelFactory.get(type).send(ctx),其余不变

**删除**
- service/channel/MockChannelSender   职责被 Handler 体系取代,删前 Find Usages 确认无残留引用

## 五、踩坑表
| # | 坑 | 解决 |
|---|---|---|
| 26 | 【你来填:如启动报 IllegalStateException 重复 Handler,说明两类 support() 返回了同一类型】 | 检查四个 Handler 的 support() 各返回不同 ChannelType |
| 27 | 【你来填:如注册 Handler 数 <4,说明有类漏标 @Component 或包没被扫描】 | 补 @Component / 确认包在扫描路径下 |
| 28 | 抽象基类依赖注入:基类用构造注入会牵连所有子类写 super 构造器 | 基类共享依赖(faultInjector)破例用 @Autowired 字段注入,注释标明特例;子类仍无依赖 |

> 注:#26 #27 是预埋占位,实际没踩到就删掉。本次启动一次过则三坑均可删。

## 六、遗留 TODO
- send 当前返回 boolean。Day15 做重试时需要「为什么失败/能否重试」,届时升级为 ChannelSendResult(本次故意先简单,迭代演进)
- 四个渠道目前 doMockSend 只打日志。Day13 起逐个换真实发送(优先邮件 JavaMail)
- ChannelFaultInjector / ChannelDebugController 仍是测试件,Day13 接真渠道后评估删除
- ChannelSendContext 已预留 channelConfig(含 configJson),Day13 真发从这里取密钥/templateCode

## 七、验证结果(端到端)

### 启动日志(策略自动注册)
```
【渠道工厂】已注册 4 个渠道 Handler: [SMS, EMAIL, PUSH, WECHAT]   【你来填:确认这行出现且为4个】
```

### 发送日志(证明走进了 Handler,不是老 MockChannelSender)
实测(T100 短信):
```
【路由】选中 provider=aliyun(样本不足0/10,视为健康)        ← Day11 路由层
【短信Mock】to=13800000019, content=测试测试内容            ← SmsChannelHandler.doMockSend(子类)
【短信发送】provider=aliyun, messageId=MSG_604..., 结果=成功 ← AbstractMockChannelHandler(基类)
```
证明:
- ✅ 日志类名 c.d.p.a.s.c.handler.SmsChannelHandler / AbstractMockChannelHandler
  → 请求真走进短信专员,不是已删的 MockChannelSender
- ✅ 两行日志一行子类一行基类 → 模板方法(基类管流程、子类管差异)正确配合
- ✅ 路由(选 provider)+ 策略(按 channelType 发)两层协作正常

### 副作用核对
- t_message status=2(成功),channel_type=1  【你来填条数】
- 健康度 ZSet 仍在埋点;ZCARD 随 5 分钟窗口正常增减
  (注:久未发送时窗口内被 ZREMRANGEBYSCORE 清空,ZCARD 归零属正常,正说明滑动窗口生效)

### 验收清单
- [ ] 启动日志「已注册 4 个渠道 Handler: [SMS, EMAIL, PUSH, WECHAT]」
- [x] 发短信日志前缀为「短信发送」+ 类名 SmsChannelHandler(走了正确 Handler)
- [x] 路由 + 策略两层协作正常
- [x] MockChannelSender 已删且编译通过
- [ ] (可选)注入 aliyun 高失败率仍能「跳过 aliyun → 选 tencent」且日志「短信发送 provider=tencent」
  → 证明 Day12 重构没破坏 Day11 降级功能

## 八、简历可写(支撑亮点1)
> 基于策略模式 + 工厂 + 模板方法重构多渠道发送:ChannelHandler 接口 + 四渠道实现,
> Spring 自动注入收集 + EnumMap 索引分发,消除 if-else,新增渠道符合开闭原则(零改动老代码);
> 故障注入与公共流程下沉抽象基类,子类只实现渠道差异。

## 九、明日计划(Day 13)
- 对接真渠道:邮件 EmailChannelHandler 接 JavaMail SMTP(QQ邮箱授权码),Demo 真发一封到任意邮箱
- 短信保持 Mock(签名需企业资质,口头说明生产接阿里云),代码结构已按真 SDK 形态铺好
- 从 ChannelSendContext.channelConfig.configJson 取发送所需配置(SMTP 账号/授权码)
- 验证:新建邮件模板(channel_type=2)+ 配邮件渠道(channel_config),端到端真发到邮箱