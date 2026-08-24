package com.agrawalpulse.common.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

// Not profile-gated (unlike LocalJwtConfig): hashing is a universal concern, not a local-dev
// stand-in for something Cognito does instead. Cognito owns credential storage/verification in
// dev/staging/prod, but if this codebase ever stores a password of its own anywhere, it must
// always be through BCrypt - there is no scenario in which a plaintext or reversibly-encrypted
// password is acceptable.
@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
