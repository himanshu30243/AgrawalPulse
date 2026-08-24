package com.agrawalpulse.family;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// scanBasePackages must include com.agrawalpulse.common - SecurityConfig, OpenApiConfig,
// GlobalExceptionHandler, CurrentTenantResolver, and the security.local dev-token issuer all
// live there and are not on the default com.agrawalpulse.family component-scan path.
@SpringBootApplication(scanBasePackages = {"com.agrawalpulse.family", "com.agrawalpulse.common"})
public class FamilyServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FamilyServiceApplication.class, args);
    }
}
