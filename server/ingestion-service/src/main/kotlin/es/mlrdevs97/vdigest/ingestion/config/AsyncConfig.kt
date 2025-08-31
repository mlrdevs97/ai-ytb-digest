package es.mlrdevs97.vdigest.ingestion.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.Executors
import java.util.concurrent.ExecutorService

@Configuration
class AsyncConfig {
    @Bean
    fun ingestionExecutor(): ExecutorService = Executors.newFixedThreadPool(
        Runtime.getRuntime().availableProcessors().coerceAtLeast(2)
    )
}
