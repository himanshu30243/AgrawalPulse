package com.agrawalpulse.analytics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

// scanBasePackages must list both packages explicitly: SecurityConfig/OpenApiConfig/
// LocalTokenController/CurrentTenantResolver live in com.agrawalpulse.common, outside this
// module's own com.agrawalpulse.analytics package, and Spring Boot's default component scan only
// covers the package the @SpringBootApplication class itself sits in.
@SpringBootApplication(scanBasePackages = {"com.agrawalpulse.analytics", "com.agrawalpulse.common"})
@EnableCaching
public class AnalyticsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AnalyticsServiceApplication.class, args);
    }
}
