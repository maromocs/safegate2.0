package com.SafeGate.config;

import com.SafeGate.filter.RequestPreprocessorFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FilterConfig {

    @Bean
    public FilterRegistrationBean<RequestPreprocessorFilter> requestPreprocessorFilter() {
        FilterRegistrationBean<RequestPreprocessorFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new RequestPreprocessorFilter());
        registrationBean.addUrlPatterns("/*");
        registrationBean.setOrder(1);
        return registrationBean;
    }
}