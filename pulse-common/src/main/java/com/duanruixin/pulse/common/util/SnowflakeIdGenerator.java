package com.duanruixin.pulse.common.util;

/**
 * Snowflake 雪花算法 ID 生成器
 *
 * 64 位结构:
 *   0 | 41位时间戳 | 5位数据中心ID | 5位机器ID | 12位序列号
 */
public class SnowflakeIdGenerator {

    /** 起始时间戳(2026-01-01 00:00:00) */
    private static final long START_TIMESTAMP = 1767225600000L;

    /** 各部分占位数 */
    private static final long DATACENTER_BITS = 5L;
    private static final long MACHINE_BITS = 5L;
    private static final long SEQUENCE_BITS = 12L;

    /** 各部分最大值 */
    private static final long MAX_DATACENTER_ID = ~(-1L << DATACENTER_BITS);  // 31
    private static final long MAX_MACHINE_ID = ~(-1L << MACHINE_BITS);        // 31
    private static final long MAX_SEQUENCE = ~(-1L << SEQUENCE_BITS);          // 4095

    /** 各部分左移位数 */
    private static final long MACHINE_SHIFT = SEQUENCE_BITS;                   // 12
    private static final long DATACENTER_SHIFT = SEQUENCE_BITS + MACHINE_BITS; // 17
    private static final long TIMESTAMP_SHIFT = DATACENTER_SHIFT + DATACENTER_BITS; // 22

    /** 数据中心 ID 和机器 ID(部署时配置) */
    private final long datacenterId;
    private final long machineId;

    /** 当前序列号 */
    private long sequence = 0L;

    /** 上次生成 ID 的时间戳 */
    private long lastTimestamp = -1L;

    public SnowflakeIdGenerator(long datacenterId, long machineId) {
        if (datacenterId > MAX_DATACENTER_ID || datacenterId < 0) {
            throw new IllegalArgumentException("datacenterId 必须在 0~" + MAX_DATACENTER_ID + " 之间");
        }
        if (machineId > MAX_MACHINE_ID || machineId < 0) {
            throw new IllegalArgumentException("machineId 必须在 0~" + MAX_MACHINE_ID + " 之间");
        }
        this.datacenterId = datacenterId;
        this.machineId = machineId;
    }

    /**
     * 生成下一个 ID(线程安全)
     */
    public synchronized long nextId() {
        long currentTimestamp = System.currentTimeMillis();

        // 时钟回拨保护
        if (currentTimestamp < lastTimestamp) {
            throw new RuntimeException("时钟回拨,拒绝生成 ID,差值=" + (lastTimestamp - currentTimestamp) + "ms");
        }

        // 同一毫秒内,序列号自增
        if (currentTimestamp == lastTimestamp) {
            sequence = (sequence + 1) & MAX_SEQUENCE;
            // 序列号用完,等到下一毫秒
            if (sequence == 0) {
                currentTimestamp = waitNextMillis(lastTimestamp);
            }
        } else {
            // 新的一毫秒,序列号重置
            sequence = 0L;
        }

        lastTimestamp = currentTimestamp;

        // 拼装 64 位 ID
        return ((currentTimestamp - START_TIMESTAMP) << TIMESTAMP_SHIFT)
                | (datacenterId << DATACENTER_SHIFT)
                | (machineId << MACHINE_SHIFT)
                | sequence;
    }

    /**
     * 自旋等待到下一毫秒
     */
    private long waitNextMillis(long lastTimestamp) {
        long timestamp = System.currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }


    public static void main(String[] args) {
        SnowflakeIdGenerator gen = new SnowflakeIdGenerator(1, 1);

        // 生成 10 个 ID
        for (int i = 0; i < 10; i++) {
            System.out.println(gen.nextId());
        }

        // 性能测试:1 秒能生成多少 ID
        long start = System.currentTimeMillis();
        int count = 0;
        while (System.currentTimeMillis() - start < 1000) {
            gen.nextId();
            count++;
        }
        System.out.println("\n1 秒生成 ID 数: " + count);
    }
}