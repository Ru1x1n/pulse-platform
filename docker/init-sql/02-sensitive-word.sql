-- ============================================
-- t_sensitive_word 敏感词表(Day 8)
-- ============================================
CREATE TABLE IF NOT EXISTS t_sensitive_word (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    word        VARCHAR(128) NOT NULL COMMENT '敏感词',
    category    VARCHAR(32)  DEFAULT 'TEST' COMMENT '分类:TEST/POLITICS/ABUSE 等',
    enabled     TINYINT      NOT NULL DEFAULT 1 COMMENT '是否启用 1启用 0停用',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted  TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0未删 1已删',
    PRIMARY KEY (id),
    UNIQUE KEY uk_word (word)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='敏感词表';

-- 中性测试词:故意覆盖各种边界场景
INSERT INTO t_sensitive_word (word, category) VALUES
('赌博', 'TEST'),          -- 普通双字
('赌场', 'TEST'),          -- 和"赌博"共享前缀"赌"(测 Trie 分叉)
('诈骗', 'TEST'),
('刷单', 'TEST'),
('套现', 'TEST'),
('暴力', 'TEST'),
('违禁品', 'TEST'),        -- 三字词
('代开发票', 'TEST'),      -- 四字词(测长词)
('烟', 'TEST'),            -- 单字词(测单字命中)
('烟草专卖', 'TEST'),      -- 和"烟"共享前缀"烟"(测短词被长词包含)
('casino', 'TEST'),        -- 英文(测大小写处理)
('VPN', 'TEST');           -- 英文大写