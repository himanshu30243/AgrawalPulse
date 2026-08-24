package com.agrawalpulse.matrimony.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class FamilyServiceClientConfig {

    @Bean
    public RestClient familyServiceRestClient(
            RestClient.Builder builder,
            @Value("${agrawalpulse.services.family-service.base-url}") String familyServiceBaseUrl) {
        return builder.baseUrl(familyServiceBaseUrl).build();
    }
}
