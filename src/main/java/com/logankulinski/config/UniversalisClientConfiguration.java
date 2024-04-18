package com.logankulinski.config;

import de.chojo.universalis.rest.UniversalisRest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UniversalisClientConfiguration {
    @Bean
    public UniversalisRest universalisRest() {
        return UniversalisRest.builder()
                              .build();
    }
}
