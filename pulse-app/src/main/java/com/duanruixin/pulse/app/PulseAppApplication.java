package com.duanruixin.pulse.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication(scanBasePackages = "com.duanruixin.pulse")
public class PulseAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(PulseAppApplication.class, args);
        System.out.println("====================================");
        System.out.println(":::: Pulse App Service Started :::: 8081");
        System.out.println("====================================");
    }
}