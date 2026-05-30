package com.duanruixin.pulse.common.util;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SnowflakeIdGeneratorTest {

    @Test
    void should_generate_unique_ids() {
        SnowflakeIdGenerator gen = new SnowflakeIdGenerator(1, 1);
        Set<Long> ids = new HashSet<>();
        int count = 10000;

        for (int i = 0; i < count; i++) {
            ids.add(gen.nextId());
        }

        assertEquals(count, ids.size(), "10000 个 ID 应该全部唯一");
    }

    @Test
    void should_generate_increasing_ids() {
        SnowflakeIdGenerator gen = new SnowflakeIdGenerator(1, 1);
        long prev = gen.nextId();
        for (int i = 0; i < 100; i++) {
            long current = gen.nextId();
            assertTrue(current > prev, "ID 应该严格递增");
            prev = current;
        }
    }

    @Test
    void should_reject_invalid_datacenter_id() {
        assertThrows(IllegalArgumentException.class,
                () -> new SnowflakeIdGenerator(32, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new SnowflakeIdGenerator(-1, 1));
    }
}