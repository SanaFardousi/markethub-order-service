package com.markethub.orders.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling       // without this, @Scheduled methods are ignored
public class KafkaConfig {
}
