package com.softwareProduct.config;

import com.softwareProduct.dto.GlobalPayloadDataDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.annotation.RequestScope;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

@Configuration
public class CustomWebConfig extends WebMvcConfigurerAdapter {

    @Autowired
    private JwtInterceptor jwtInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor);
    }

    @Bean
    @RequestScope
    public GlobalPayloadDataDTO globalPayloadDataDTO() {
        return new GlobalPayloadDataDTO();
    }

    @Bean
    public JwtInterceptor jwtInterceptor() {
        return new JwtInterceptor(globalPayloadDataDTO());
    }
}
