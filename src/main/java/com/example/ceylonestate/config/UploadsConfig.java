package com.example.ceylonestate.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;

import java.util.Map;

/** Routes /uploads/** to UploadRequestHandler (photos stored in the database). */
@Configuration
public class UploadsConfig {

    @Bean
    public SimpleUrlHandlerMapping uploadsHandlerMapping(UploadRequestHandler handler) {
        // order 1: ahead of Spring's static-resource mapping, which always comes last
        return new SimpleUrlHandlerMapping(Map.of("/uploads/**", handler), 1);
    }
}
