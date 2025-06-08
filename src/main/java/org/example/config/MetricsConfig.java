package org.example.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

    @Bean
    public Counter trackingNumberGeneratedCounter(MeterRegistry meterRegistry) {
        return Counter.builder("tracking_numbers_generated_total")
                .description("Number of tracking numbers generated")
                .register(meterRegistry);
    }

    @Bean
    public Timer trackingNumberGenerationTimer(MeterRegistry meterRegistry) {
        return Timer.builder("tracking_number_generation_duration")
                .description("Generation time metrics")
                .register(meterRegistry);
    }

    @Bean
    public Counter trackingNumberErrorCounter(MeterRegistry meterRegistry) {
        return Counter.builder("tracking_number_errors_total")
                .description("Generation error count")
                .register(meterRegistry);
    }
}
