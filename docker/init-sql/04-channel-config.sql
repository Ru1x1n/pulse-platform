-- Day11 渠道配置测试数据：app_id=2 短信渠道，aliyun(优先) + tencent(备用)
-- priority 数字越大越优先；config_json 在 Day11 Mock 阶段用不到，占位 '{}'
INSERT INTO t_channel_config (app_id, channel_type, channel_name, provider, config_json, priority, status)
VALUES
(2, 1, '阿里云短信', 'aliyun',  '{}', 20, 1),
(2, 1, '腾讯云短信', 'tencent', '{}', 10, 1);