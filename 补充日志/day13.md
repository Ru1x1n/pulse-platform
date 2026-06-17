# Day 13 对接真渠道:邮件真发(JavaMail + QQ邮箱 SMTP)

> 一句话:把 Day12 邮件 Handler 的「假发(打日志)」换成 JavaMail 真发,Demo 真的发出一封邮件并收到;
> 短信/推送/微信保持 Mock。得益于 Day12 策略模式,接真渠道只改了 EmailChannelHandler 一个类。

---

## 一、今日目标
- pulse-app 接入 spring-boot-starter-mail
- yml 配置 QQ 邮箱 SMTP(465 + SSL + 授权码)
- EmailChannelHandler 改为真发:JavaMailSender 发邮件,成败由发送结果决定
- 短信/推送/微信三个 Handler 保持 Mock 不动(开闭原则实证)
- 建邮件模板(T200, channel_type=2)+ 邮件渠道配置(qq-mail)
- 端到端:Postman 发 → 真邮箱收到邮件

## 二、核心设计

### 1. 难点:真发的成败不能用「随机判定」
Day12 基类 AbstractMockChannelHandler.send() 是「随机骰子」决定成败(Mock 渠道适用)。
但邮件真发的成败必须由「邮件到底发出去没有」决定——发成功 true,SMTP 抛异常 false。
若邮件 Handler 还走基类随机判定,会出现「真发出去了却被随机数判失败」的荒唐情况。

**解法:邮件 Handler 脱离基类,直接 implements ChannelHandler 自己写 send()。**
```
Day12: 四个 Handler 都 extends AbstractMockChannelHandler(随机判定)
Day13:
  Sms / Push / Wechat   extends AbstractMockChannelHandler   ← 仍 Mock,不动
  Email                 implements ChannelHandler            ← 改:真发,成败由结果决定
```
Mock 渠道继续享受基类随机故障注入(降级测试要用),邮件渠道走真实逻辑,两套互不干扰,
且没动基类、没动其他三个 Handler —— 策略模式的好处再次兑现。

### 2. 保留故障注入开关(让邮件渠道也能参与降级测试)
邮件 Handler 脱离基类后,仍先查一次 ChannelFaultInjector:
- 手动注入了失败率 → 按概率模拟失败(不真发),供 Day11 降级测试
- 没注入(失败率0) → 真发
  这样既能真发,又能用注入方式测邮件渠道的降级切换。

### 3. 邮箱账密放 yml(方案A)
- spring.mail.* 标准配置,JavaMailSender 自动装配,开箱即用
- 发件人 from 用 @Value("${spring.mail.username}") 取,保证 = 登录账号(QQ 要求一致否则拒发)
- 多租户「每 app 自己邮箱」(方案B,从 config_json 取)留作后续升级,ChannelSendContext 已预留 channelConfig

## 三、链路位置(只改邮件 Handler 内部,其余全不动)
```
MessageConsumer.onMessage:
  PENDING→SENDING
  查模板拿 channelType=2(邮件)
  ChannelRouter.route(...) → provider=qq-mail
  channelFactory.get(2).send(ctx) → EmailChannelHandler.send() → JavaMailSender 真发  ★Day13
  ChannelHealthService.record(...) 埋点
  成功 SENDING→SUCCESS
```

## 四、本次新增 / 改动
**新增**
- pom: spring-boot-starter-mail 依赖
- yml: spring.mail.*(host=smtp.qq.com, port=465, SSL, username=完整邮箱, password=授权码)
- docker/init-sql/05-email-test.sql: T200 邮件模板 + qq-mail 邮件渠道配置

**改动**
- EmailChannelHandler:extends 基类 → implements ChannelHandler,doMockSend 假发 → JavaMail 真发
  (SimpleMailMessage 纯文本;try/catch 兜 SMTP 异常,成败如实返回 boolean)

**未动(开闭原则实证)**
- SmsChannelHandler / PushChannelHandler / WechatChannelHandler / AbstractMockChannelHandler
- ChannelFactory / ChannelRouter / ChannelHealthService / MessageConsumer

## 五、踩坑表
| # | 坑 | 解决 |
|---|---|---|
| 29 | 【你来填:如真发报 535 Login Fail】 | password 必须填 SMTP 授权码不是 QQ 登录密码;授权码去掉空格 |
| 30 | 【你来填:如认证失败/501 发件人错误】 | username 填完整邮箱(带@qq.com);from 用 @Value 取的 username,别写别的 |
| 31 | 【你来填:如日志成功但收不到】 | 翻垃圾邮件箱(新发件人常被归类垃圾) |

> 注:#29~31 为真发常见坑预埋。本次一次成功则按实际删除未踩到的。

## 六、遗留 TODO
- 邮件目前 SimpleMailMessage 纯文本 + 固定标题「【Pulse】消息通知」。后续可换 MimeMessage 发 HTML、标题模板化
- 授权码明文在 yml,勿提交公开仓库。后续改 ${MAIL_PASSWORD} 环境变量 / Nacos 配置中心
- 短信真发需企业资质(签名审核),Demo 保持 Mock,口头说明生产接阿里云/腾讯云 SDK
- 多租户每 app 独立发件邮箱(方案B,从 channel_config.config_json 取)未做,接口已预留
- 邮件发送是同步阻塞(SMTP 往返),高并发下会拖慢消费。后续可评估线程池 / 异步(当前 Demo 量级无碍)

## 七、验证结果(端到端)

### 发送日志
```
【路由】选中 provider=qq-mail(...)
【邮件发送】provider=qq-mail, messageId=MSG_xxx, to=xxx, 结果=成功
```

### ★高光验收:真邮箱收到邮件 ✅
- 标题:【Pulse】消息通知
- 正文:你好 段锐昕,这是一条来自 Pulse 的测试邮件:Pulse 真发测试成功
- 状态:真发出 + 真收到(Demo 第一个能真实发送的渠道)

### 副作用核对
- t_message:邮件那条 status=2(成功)、channel_type=2  【你来填】
- ZCARD pulse:channel:total:2:qq-mail 有埋点  【你来填】

### 验收清单
- [x] pom 加 spring-boot-starter-mail,Maven 刷新成功
- [x] yml 配好 QQ 邮箱(完整邮箱 + 授权码 + 465/SSL)
- [x] EmailChannelHandler 改 implements 真发,其他三个 Handler 没动
- [x] T200 模板 + qq-mail 渠道测试数据导入
- [x] Postman 发 T200,日志「邮件发送 结果=成功」
- [x] 真邮箱收到邮件(高光验收点)
- [ ] t_message 邮件那条 status=2、channel_type=2  【跑一下确认】
- [ ] 短信(T100)仍正常 Mock(没被邮件改动影响)  【可选回归】

## 八、简历可写
> 对接真实第三方渠道:基于 JavaMail + QQ邮箱 SMTP 实现邮件真实发送,发送成败由 SMTP 结果如实驱动状态机;
> 邮件 Handler 脱离 Mock 基类直接实现策略接口、其余渠道零改动——开闭原则在接入真渠道时的实证,
> 接一个真渠道仅改动一个类。

## 九、明日计划(Day 14)
- Consumer 真正消费打通:把 Day10 残留的「状态机骨架」补成完整业务流
  (当前 onMessage 已含 路由→策略发送→埋点→状态机,Day14 核对是否需补 频控/敏感词 在消费端的位置)
- 端到端回归:T100 短信 Mock + T200 邮件真发 两条链路都跑通,确认 Day6~13 整链稳定
- 为 Day15(退避重试 + 死信)做准备:确认失败(status=3)的消息当前去向,梳理重试切入点