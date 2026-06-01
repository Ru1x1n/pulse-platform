package com.duanruixin.pulse.app.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {

    @Value("${redisson.address}")
    private String address;

    @Value("${redisson.password}")
    private String password;

    @Value("${redisson.database}")
    private int database;

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer()
                .setAddress(address)
                .setPassword(password)
                .setDatabase(database)
                .setConnectionPoolSize(64)
                .setConnectionMinimumIdleSize(10);
        return Redisson.create(config);
    }
}