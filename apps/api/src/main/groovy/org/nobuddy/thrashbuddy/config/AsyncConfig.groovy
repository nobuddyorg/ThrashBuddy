package org.nobuddy.thrashbuddy.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync

import java.util.concurrent.Executors
import java.util.concurrent.Executor

@Configuration
@EnableAsync
class AsyncConfig {
    // Only test start/stop ever run here (one at a time in practice, since
    // TestExecutionService gates them on status) - a small fixed pool avoids
    // the unbounded thread growth of newCachedThreadPool.
    @Bean(name = "blockingExecutor")
    Executor blockingExecutor() {
        return Executors.newFixedThreadPool(2)
    }
}

