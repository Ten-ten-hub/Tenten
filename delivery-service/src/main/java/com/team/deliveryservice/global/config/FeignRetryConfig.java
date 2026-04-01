package com.team.deliveryservice.global.config;

import feign.Retryer;
import org.springframework.context.annotation.Bean;

public class FeignRetryConfig {

    @Bean
    public Retryer retryer() {
        return new Retryer.Default(1000, 2000, 3);
    }
}
