package com.iisl.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.iisl.controller.BaseFilter;
import com.iisl.controller.HSTSFilter;

@Configuration
public class WebFilterConfig {

    @Bean
    public FilterRegistrationBean<BaseFilter> baseFilter() {
        FilterRegistrationBean<BaseFilter> reg = new FilterRegistrationBean<>();
        reg.setFilter(new BaseFilter());
        reg.addUrlPatterns("/*");
        reg.setOrder(1);
        return reg;
    }

    @Bean
    public FilterRegistrationBean<HSTSFilter> hstsFilter() {
        FilterRegistrationBean<HSTSFilter> reg = new FilterRegistrationBean<>();
        reg.setFilter(new HSTSFilter());
        reg.addUrlPatterns("/*");
        reg.setOrder(2);

        reg.addInitParameter("maxAgeSeconds", "31536000");
        reg.addInitParameter("includeSubDomains", "true");
        reg.addInitParameter("preload", "true");

        return reg;
    }
}
