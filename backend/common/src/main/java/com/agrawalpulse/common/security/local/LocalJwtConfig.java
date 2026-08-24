package com.agrawalpulse.common.security.local;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

// DEV-ONLY JWT issuer/verifier. This bean set replaces real Cognito JWK-based validation with
// a symmetric HMAC key generated from local config, purely so the rest of the app (JwtRolesConverter,
// CurrentTenantResolver, every @PreAuthorize) never has to branch on environment.
//
// This class MUST remain gated behind @Profile("local"). If it is ever active in dev/staging/prod,
// anyone who knows (or brute-forces) the shared secret can mint tokens with arbitrary chapter_id /
// roles claims and bypass tenant isolation entirely.
@Configuration
@Profile("local")
public class LocalJwtConfig {

    @Value("${agrawalpulse.jwt.local.secret}")
    private String secret;

    @Bean
    public SecretKeySpec localJwtSecretKey() {
        return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Bean
    public JwtDecoder jwtDecoder(SecretKeySpec localJwtSecretKey) {
        return NimbusJwtDecoder.withSecretKey(localJwtSecretKey).macAlgorithm(org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256).build();
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKeySpec localJwtSecretKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(localJwtSecretKey));
    }
}
