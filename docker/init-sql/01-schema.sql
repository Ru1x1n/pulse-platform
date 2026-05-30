-- ============================================
-- Pulse Platform 核心库表
-- 版本:v1.0 (Day 2)
-- ============================================

USE pulse;

-- 1. 租户表
CREATE TABLE IF NOT EXISTS `t_tenant` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '租户ID',
    `tenant_name` VARCHAR(100) NOT NULL COMMENT '租户名称',
    `tenant_code` VARCHAR(50) NOT NULL COMMENT '租户编码(全局唯一)',
    `contact_name` VARCHAR(50) DEFAULT NULL COMMENT '联系人',
    `contact_email` VARCHAR(100) DEFAULT NULL COMMENT '联系邮箱',
    `contact_phone` VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态:1-正常 0-停用',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除:0-未删 1-已删',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_code` (`tenant_code`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='租户表';


-- 2. 应用表
CREATE TABLE IF NOT EXISTS `t_app` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '应用ID',
    `tenant_id` BIGINT NOT NULL COMMENT '所属租户',
    `app_name` VARCHAR(100) NOT NULL COMMENT '应用名称',
    `app_key` VARCHAR(64) NOT NULL COMMENT 'API Key(对外暴露)',
    `app_secret` VARCHAR(128) NOT NULL COMMENT 'API Secret(签名密钥)',
    `daily_quota` INT NOT NULL DEFAULT 10000 COMMENT '每日发送配额',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态:1-正常 0-停用',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_app_key` (`app_key`),
    KEY `idx_tenant_id` (`tenant_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='应用表';


-- 3. 渠道配置表
CREATE TABLE IF NOT EXISTS `t_channel_config` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `app_id` BIGINT NOT NULL COMMENT '关联的应用ID',
    `channel_type` TINYINT NOT NULL COMMENT '渠道类型:1-短信 2-邮件 3-App推送 4-微信公众号',
    `channel_name` VARCHAR(50) NOT NULL COMMENT '渠道名称',
    `provider` VARCHAR(50) NOT NULL COMMENT '服务商:aliyun/tencent/qq-mail等',
    `config_json` TEXT NOT NULL COMMENT '配置JSON(accessKey/secretKey/templateCode等)',
    `priority` INT NOT NULL DEFAULT 10 COMMENT '优先级(数字越大越优先)',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态:1-启用 0-禁用',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_app_channel` (`app_id`, `channel_type`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='渠道配置表';


-- ============================================
-- 测试数据(便于开发调试)
-- ============================================
INSERT INTO `t_tenant` (`tenant_name`, `tenant_code`, `contact_name`, `contact_email`)
VALUES ('测试租户', 'TENANT_TEST', '段锐昕', 'duanruixin@virginia.edu');

INSERT INTO `t_app` (`tenant_id`, `app_name`, `app_key`, `app_secret`, `daily_quota`)
VALUES (1, '测试应用', 'pulse_demo_key', 'pulse_demo_secret_12345678', 10000);