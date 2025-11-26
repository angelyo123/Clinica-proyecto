package com.historias_clinicas.hc.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class IAExecutorConfig {

    @Bean
    public ExecutorService iaExecutor() {
        return Executors.newFixedThreadPool(8);
    }
}