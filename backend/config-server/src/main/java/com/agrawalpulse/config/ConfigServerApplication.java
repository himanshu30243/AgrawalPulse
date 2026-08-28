package com.agrawalpulse.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

/**
 * Spring Cloud Config Server - Centralized configuration management.
 *
 * Serves configuration files to all microservices instead of each service
 * having its own application.yml. Enables dynamic configuration updates
 * without restarting services.
 *
 * Runs on port 8888 by default.
 *
 * Configuration repository: file://config-repo (local) or Git URL (production)
 *
 * Access patterns:
 *   GET /user-service/local           → user-service-local.yml
 *   GET /family-service/prod          → family-service-prod.yml
 *   GET /api-gateway/default          → api-gateway.yml
 *
 * Benefits:
 *   - Single source of truth for all configurations
 *   - No need to rebuild/redeploy when config changes
 *   - Environment-specific configs (local/dev/staging/prod)
 *   - Audit trail (Git history of all config changes)
 */
@SpringBootApplication
@EnableConfigServer
public class ConfigServerApplication {

  public static void main(String[] args) {
    SpringApplication.run(ConfigServerApplication.class, args);
  }
}
