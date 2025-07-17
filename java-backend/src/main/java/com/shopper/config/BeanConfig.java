package com.shopper.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;

@Configuration
public class BeanConfig {

    @Bean
    @Primary
    public ParameterNameDiscoverer parameterNameDiscoverer() {
        return new DefaultParameterNameDiscoverer();
    }
} 