package com.agrawalpulse.eureka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Eureka Service Discovery Server.
 *
 * Central registry where all microservices register and discover each other.
 * Runs on port 8761 by default.
 *
 * All 7 services register here:
 *   - api-gateway
 *   - user-service
 *   - family-service
 *   - membership-service
 *   - matrimony-service
 *   - event-service
 *   - analytics-service
 *
 * Benefits:
 *   - Dynamic service discovery (no hardcoded URLs)
 *   - Load balancing across service instances
 *   - Service health monitoring
 *   - Automatic failover
 */
@SpringBootApplication
@EnableEurekaServer
public class EurekaServerApplication {

  public static void main(String[] args) {
    SpringApplication.run(EurekaServerApplication.class, args);
  }
}
