package com.agrawalpulse.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI agrawalPulseOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("AgrawalPulse API")
                        .version("v1")
                        .description("""
                                Local profile: obtain a token from POST /api/v1/local-auth/token first,
                                then click Authorize below and paste it as a Bearer token.
                                Cloud profiles: this endpoint doesn't exist - auth goes through Cognito instead."""))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                                .name(BEARER_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
