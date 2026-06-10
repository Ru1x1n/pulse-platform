package com.duanruixin.pulse.app.service.audit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SensitiveWordTrieTest {

    private SensitiveWordTrie trie;

    @BeforeEach
    void setUp() {
        trie = new SensitiveWordTrie();
        trie.addWord("赌博");
        trie.addWord("赌场");      // 共享前缀"赌"
        trie.addWord("烟");        // 单字
        trie.addWord("烟草专卖");  // 短词被长词包含场景
        trie.addWord("casino");    // 英文
        trie.addWord("VPN");       // 大写
    }

    @Test
    void 命中普通词() {
        assertEquals("赌博", trie.findFirst("欢迎来赌博"));
    }

    @Test
    void 共享前缀的两个词都能命中() {
        assertTrue(trie.contains("去赌场玩"));
        assertTrue(trie.contains("玩赌博"));
    }

    @Test
    void 单字词命中() {
        assertTrue(trie.contains("我抽烟"));
    }

    @Test
    void 大小写不敏感() {
        assertTrue(trie.contains("用Casino网站"));
        assertTrue(trie.contains("翻墙用vpn"));
    }

    @Test
    void 干净文本不误判() {
        assertNull(trie.findFirst("您的验证码是8888,5分钟内有效"));
        assertFalse(trie.contains("正常的订单通知内容"));
    }

    @Test
    void 空和null安全() {
        assertNull(trie.findFirst(null));
        assertNull(trie.findFirst(""));
    }

    @Test
    void 命中第一个就返回() {
        // "赌博"在前,应返回赌博而非后面的烟
        assertEquals("赌博", trie.findFirst("先赌博再抽烟"));
    }
}