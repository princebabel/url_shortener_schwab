package com.schwab.urlshortener.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("URL Shortener API")
                        .version("1.0.0")
                        .description("Production-ready URL shortening, redirect, analytics, and dashboard APIs")
                        .contact(new Contact()
                                .name("Charles Schwab Engineering")
                                .email("engineering@schwab.example")))
                .tags(List.of(
                        new Tag().name("URLs").description("Create, manage, and resolve short URLs"),
                        new Tag().name("Analytics").description("Retrieve analytics and dashboard summaries"),
                        new Tag().name("Health").description("Health and availability endpoints")));
    }

}

