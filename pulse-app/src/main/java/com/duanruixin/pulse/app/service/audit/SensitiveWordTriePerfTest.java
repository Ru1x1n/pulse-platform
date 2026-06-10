package com.duanruixin.pulse.app.service.audit;

import org.junit.jupiter.api.Test;

class SensitiveWordTriePerfTest {

    @Test
    void 性能_1000字检测应在1ms内() {
        // 1. build 一个稍大的词库(模拟真实规模)
        SensitiveWordTrie trie = new SensitiveWordTrie();
        for (int i = 0; i < 1000; i++) {
            trie.addWord("敏感词" + i);
        }
        trie.addWord("赌博");

        // 2. 造 1000 字文本,末尾埋一个敏感词(最坏情况:扫到最后才命中)
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 995; i++) {
            sb.append("正");
        }
        sb.append("赌博"); // 第 996 字才命中
        String text = sb.toString();

        // 3. 预热(让 JIT 编译生效)
        for (int i = 0; i < 10000; i++) {
            trie.findFirst(text);
        }

        // 4. 正式测:跑 1000 次取平均
        long start = System.nanoTime();
        int rounds = 1000;
        for (int i = 0; i < rounds; i++) {
            trie.findFirst(text);
        }
        long costNs = (System.nanoTime() - start) / rounds;
        double costMs = costNs / 1_000_000.0;

        System.out.printf("1000字检测平均耗时: %.4f ms (%d ns)%n", costMs, costNs);
        // 单次应远小于 1ms
        org.junit.jupiter.api.Assertions.assertTrue(costMs < 1.0,
                "检测耗时超过 1ms: " + costMs);
    }
}