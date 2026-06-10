package com.duanruixin.pulse.app.service.audit;

import com.duanruixin.pulse.app.entity.SensitiveWord;
import com.duanruixin.pulse.app.mapper.SensitiveWordMapper;
import com.duanruixin.pulse.common.exception.BusinessException;
import com.duanruixin.pulse.common.result.ErrorCode;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 敏感词审核服务
 * 启动时从 DB build Trie 进内存,检测全走内存(不查库)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final SensitiveWordMapper sensitiveWordMapper;

    /** 内存中的 Trie,volatile 保证 reload 后其它线程立即可见 */
    private volatile SensitiveWordTrie trie = new SensitiveWordTrie();

    /**
     * 启动时构建 Trie
     */
    @PostConstruct
    public void init() {
        reload();
    }

    /**
     * 从 DB 重新加载敏感词,重建 Trie
     * (后续可做成接口,加完词热刷新;Day8 先提供方法)
     */
    public void reload() {
        SensitiveWordTrie newTrie = new SensitiveWordTrie();
        List<SensitiveWord> words = sensitiveWordMapper.selectList(
                com.baomidou.mybatisplus.core.toolkit.Wrappers.<SensitiveWord>lambdaQuery()
                        .eq(SensitiveWord::getEnabled, 1)
        );
        for (SensitiveWord w : words) {
            newTrie.addWord(w.getWord());
        }
        // 原子替换:旧 Trie 被新的整体替换,检测中的请求不受影响
        this.trie = newTrie;
        log.info("敏感词 Trie 构建完成,共加载 {} 个词", words.size());
    }

    /**
     * 检测内容,命中敏感词直接抛业务异常(拒发)
     */
    public void check(String content) {
        String hit = trie.findFirst(content);
        if (hit != null) {
            log.warn("内容命中敏感词: word={}", hit);
            throw new BusinessException(ErrorCode.SENSITIVE_WORD_DETECTED,
                    "内容包含敏感词: " + hit);
        }
    }

    /**
     * 只检测不抛异常(给测试/其它场景用)
     */
    public String detect(String content) {
        return trie.findFirst(content);
    }
}