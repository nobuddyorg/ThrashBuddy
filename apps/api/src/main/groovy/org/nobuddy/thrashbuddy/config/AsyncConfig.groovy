package org.nobuddy.thrashbuddy.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync

import java.util.concurrent.Executors
import java.util.concurrent.Executor

@Configuration
@EnableAsync
class AsyncConfig {
    @Bean(name = "blockingExecutor")
    Executor blockingExecutor() {
        return Executors.newCachedThreadPool()
    }
}

