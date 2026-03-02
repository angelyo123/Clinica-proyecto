package com.historias_clinicas.hc.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class IAExecutorConfig {

    @Bean(name = "visionExecutor")
    public ExecutorService visionExecutor() {
        return Executors.newFixedThreadPool(8);
    }

    @Bean(name = "deepSeekExecutor")
    public ExecutorService deepSeekExecutor() {
        return Executors.newFixedThreadPool(3);
    }
}