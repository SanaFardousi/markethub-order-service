package com.markethub.orders.config;

import org.kie.api.KieServices;
import org.kie.api.builder.Message;
import org.kie.api.builder.Results;
import org.kie.api.runtime.KieContainer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DroolsConfig {

    @Bean
    KieContainer kieContainer() {
        KieContainer container = KieServices.Factory.get().getKieClasspathContainer();
        Results results = container.verify();
        if (results.hasMessages(Message.Level.ERROR)) {
            throw new IllegalStateException(results.toString());
        }
        return container;
    }
}
