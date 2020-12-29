package com.chan.ws.mobileappws;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // CORS for email-verification method alone
        // registry.addMapping("/users/email-verification");

        // CORS for all REST controllers but for specific methods
        // registry.addMapping("/**").allowedMethods("GET", "POST", "PUT");

        // CORS for all
        registry.addMapping("/**").allowedMethods("*").allowedOrigins("*");
    }
}
