package com.duanruixin.pulse.app.service.audit;

import java.util.HashMap;
import java.util.Map;

/**
 * 敏感词 Trie 树(前缀树)+ DFA 检测
 *
 * 设计:
 * - 每个字符是一个节点,共享前缀只存一份(赌博/赌场 共享"赌")
 * - end 标记一个完整敏感词的结尾
 * - 检测时文本只扫一遍,时间复杂度 O(文本长度),与词库大小无关
 *
 * 线程安全说明:
 * - build 阶段单线程写,build 完只读,多线程检测安全(不可变)
 */
public class SensitiveWordTrie {

    /** Trie 节点 */
    private static class TrieNode {
        Map<Character, TrieNode> children = new HashMap<>();
        boolean end = false; // 是否为某个敏感词的结尾
    }

    private final TrieNode root = new TrieNode();

    /**
     * 添加一个敏感词到树里
     */
    public void addWord(String word) {
        if (word == null || word.isEmpty()) {
            return;
        }
        TrieNode node = root;
        // 统一转小写,实现大小写不敏感(casino / CASINO 都能命中)
        String w = word.toLowerCase();
        for (int i = 0; i < w.length(); i++) {
            char c = w.charAt(i);
            node = node.children.computeIfAbsent(c, k -> new TrieNode());
        }
        node.end = true;
    }

    /**
     * 检测文本是否包含敏感词(只要命中一个就返回,快速失败)
     *
     * @return 命中的第一个敏感词,没命中返回 null
     */
    public String findFirst(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        String lower = text.toLowerCase();
        int len = lower.length();

        // 外层:以每个位置为起点尝试匹配
        for (int i = 0; i < len; i++) {
            TrieNode node = root;
            // 内层:从 i 开始往后走 Trie
            for (int j = i; j < len; j++) {
                char c = lower.charAt(j);
                node = node.children.get(c);
                if (node == null) {
                    break; // 这个起点走不下去了,换下一个起点
                }
                if (node.end) {
                    // 命中:返回原文中这一段(用原始 text 保留大小写)
                    return text.substring(i, j + 1);
                }
            }
        }
        return null;
    }

    /**
     * 检测是否包含敏感词
     */
    public boolean contains(String text) {
        return findFirst(text) != null;
    }
}