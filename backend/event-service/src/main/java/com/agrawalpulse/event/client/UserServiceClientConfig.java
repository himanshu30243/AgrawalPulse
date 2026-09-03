package com.agrawalpulse.event.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class UserServiceClientConfig {

    @Bean
    public RestClient userServiceRestClient(
            RestClient.Builder builder,
            @Value("${agrawalpulse.services.user-service.base-url}") String userServiceBaseUrl) {
        return builder.baseUrl(userServiceBaseUrl).build();
    }
}
