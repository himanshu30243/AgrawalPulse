package com.agrawalpulse.event;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// scanBasePackages must list both packages explicitly: SecurityConfig/OpenApiConfig/
// LocalTokenController/CurrentTenantResolver live in com.agrawalpulse.common, outside this
// module's own com.agrawalpulse.event package, and Spring Boot's default component scan only
// covers the package the @SpringBootApplication class itself sits in.
@SpringBootApplication(scanBasePackages = {"com.agrawalpulse.event", "com.agrawalpulse.common"})
public class EventServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EventServiceApplication.class, args);
    }
}
