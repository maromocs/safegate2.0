package com.SafeGate.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Application configuration class for defining beans.
 */
@Configuration
public class AppConfig {

    /**
     * Creates a RestTemplate bean for making HTTP requests.
     * This is used by the DatasetTestRunnerService to simulate real HTTP requests
     * during WAF testing.
     * 
     * @return A new RestTemplate instance
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}